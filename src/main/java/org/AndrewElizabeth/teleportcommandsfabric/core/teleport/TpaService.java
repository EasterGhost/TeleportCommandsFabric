package org.AndrewElizabeth.teleportcommandsfabric.core.teleport;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.core.record.AsyncRecordedLocationSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.Tpa;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class TpaService {
	private final AsyncRecordedLocationSource recordedSource;
	private final Map<UUID, Tpa.Session> sessions = new LinkedHashMap<>(1280);
	private final Map<UUID, LinkedHashSet<UUID>> targetIncoming = new HashMap<>(128);
	private final ArrayDeque<TpaTask> executionQueue = new ArrayDeque<>();

	private static final int TPA_BATCH_LIMIT = 128;

	public TpaService(AsyncRecordedLocationSource recordedSource) {
		this.recordedSource = recordedSource;
	}

	public Tpa.Session createRequest(UUID sender, UUID target, Tpa.Type type, Duration expiry) {
		UUID sessionId = UUID.randomUUID();
		long expiredTime = Util.getMillis() + expiry.toMillis();
		Tpa.Session session = new Tpa.Session(sessionId, sender, target, type, expiredTime);

		sessions.put(sessionId, session);
		targetIncoming.computeIfAbsent(target, k -> new LinkedHashSet<>()).add(sessionId);

		return session;
	}

	public Optional<Tpa.Session> getSession(UUID sessionId) {
		if (sessionId == null)
			return Optional.empty();
		return Optional.ofNullable(sessions.get(sessionId));
	}

	public Optional<Tpa.Session> getLatestIncoming(UUID targetUuid) {
		LinkedHashSet<UUID> incoming = targetIncoming.get(targetUuid);
		if (incoming == null || incoming.isEmpty()) {
			return Optional.empty();
		}

		UUID lastId = null;
		for (UUID id : incoming) {
			lastId = id;
		}
		return getSession(lastId);
	}

	public boolean acceptRequest(MinecraftServer server, UUID sessionId) {
		Tpa.Session session = sessions.get(sessionId);
		if (session == null || session.isExpired(Util.getMillis())) {
			remove(sessionId);
			return false;
		}

		ServerPlayer sender = server.getPlayerList().getPlayer(session.sender());
		ServerPlayer target = server.getPlayerList().getPlayer(session.target());

		if (sender == null || target == null) {
			remove(sessionId);
			return false;
		}

		ServerPlayer playerToMove = (session.type() == Tpa.Type.TPA) ? sender : target;
		ServerPlayer targetPlayer = (session.type() == Tpa.Type.TPA) ? target : sender;

		executionQueue.add(new TpaTask(
				playerToMove,
				(ServerLevel) targetPlayer.level(),
				targetPlayer.position(),
				targetPlayer.getYRot(),
				targetPlayer.getXRot()));

		remove(sessionId);
		return true;
	}

	public void remove(UUID sessionId) {
		if (sessionId == null)
			return;
		Tpa.Session session = sessions.remove(sessionId);
		if (session != null) {
			LinkedHashSet<UUID> incoming = targetIncoming.get(session.target());
			if (incoming != null) {
				incoming.remove(sessionId);
				if (incoming.isEmpty()) {
					targetIncoming.remove(session.target());
				}
			}
		}
	}

	public void tick() {
		int executed = 0;
		while (executed < TPA_BATCH_LIMIT && !executionQueue.isEmpty()) {
			TpaTask task = executionQueue.poll();
			if (task.player.level().getServer() != null && !task.player.isDeadOrDying()) {
				if (recordedSource != null) {
					recordedSource.recordPreviousTeleportLocation(task.player.getUUID(), task.player.blockPosition(), task.player.level().dimension())
							.whenComplete((ignored, throwable) -> {
								if (throwable != null) {
									ModConstants.LOGGER.warn("Failed to record previous teleport location for TPA", throwable);
								}
							});
				}
				task.player.teleportTo(task.world, task.pos.x, task.pos.y, task.pos.z, Set.of(), task.yRot, task.xRot, false);
			}
			executed++;
		}

		long now = Util.getMillis();
		Iterator<Map.Entry<UUID, Tpa.Session>> it = sessions.entrySet().iterator();
		while (it.hasNext()) {
			Tpa.Session session = it.next().getValue();
			if (session.isExpired(now)) {
				LinkedHashSet<UUID> targetSet = targetIncoming.get(session.target());
				if (targetSet != null) {
					targetSet.remove(session.sessionId());
					if (targetSet.isEmpty())
						targetIncoming.remove(session.target());
				}
				it.remove();
			} else {
				break;
			}
		}
	}

	public void onPlayerQuit(UUID playerUuid) {
		executionQueue.removeIf(task -> task.player.getUUID().equals(playerUuid));

		LinkedHashSet<UUID> incoming = targetIncoming.remove(playerUuid);
		if (incoming != null) {
			for (UUID id : incoming) {
				sessions.remove(id);
			}
		}

		Iterator<Tpa.Session> iterator = sessions.values().iterator();
		while (iterator.hasNext()) {
			Tpa.Session session = iterator.next();
			if (session.sender().equals(playerUuid)) {
				LinkedHashSet<UUID> targetSet = targetIncoming.get(session.target());
				if (targetSet != null) {
					targetSet.remove(session.sessionId());
				}
				iterator.remove();
			}
		}
	}

	public void clear() {
		sessions.clear();
		targetIncoming.clear();
		executionQueue.clear();
	}

	private record TpaTask(
			ServerPlayer player,
			ServerLevel world,
			Vec3 pos,
			float yRot,
			float xRot) {
	}
}
