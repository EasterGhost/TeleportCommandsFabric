package org.AndrewElizabeth.teleportcommandsfabric.core.teleport;

import org.AndrewElizabeth.teleportcommandsfabric.core.record.AsyncRecordedLocationSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportOperationManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportPreloadManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportExecutor;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportOperation;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.Tpa;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TpaRequest;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TpaTeleportPending;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TpaService {
	private final TeleportOperationManager operationManager;
	private final TeleportPreloadManager preloadManager;
	private final TeleportExecutor executor;
	private final Map<UUID, Tpa.Session> sessions = new LinkedHashMap<>(1280);
	private final Map<UUID, LinkedHashSet<UUID>> targetIncoming = new HashMap<>(128);
	private final ArrayDeque<PendingRef> acceptedQueue = new ArrayDeque<>();
	private long currentTick;

	private static final int TPA_BATCH_LIMIT = 128;

	public TpaService(AsyncRecordedLocationSource recordedSource, TeleportOperationManager operationManager,
			TeleportPreloadManager preloadManager) {
		this.operationManager = Objects.requireNonNull(operationManager, "operationManager");
		this.preloadManager = Objects.requireNonNull(preloadManager, "preloadManager");
		this.executor = new TeleportExecutor(recordedSource, this.operationManager);
	}

	public Tpa.Session createRequest(TpaRequest request) {
		Objects.requireNonNull(request, "request");
		UUID sessionId = UUID.randomUUID();
		long expiredTime = Util.getMillis() + request.expiry().toMillis();
		Tpa.Session session = new Tpa.Session(sessionId, request.senderUuid(), request.targetUuid(), request.type(), expiredTime,
				request.delayTicks(), request.cooldownMillis(), request.recordPrevious());

		sessions.put(sessionId, session);
		targetIncoming.computeIfAbsent(request.targetUuid(), ignored -> new LinkedHashSet<>()).add(sessionId);
		return session;
	}

	public Tpa.Session createRequest(UUID sender, UUID target, Tpa.Type type, Duration expiry) {
		return createRequest(TpaRequest.of(sender, target, type, expiry));
	}

	public Optional<Tpa.Session> getSession(UUID sessionId) {
		if (sessionId == null) {
			return Optional.empty();
		}
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

	public CompletableFuture<TeleportStatus> acceptRequest(MinecraftServer server, UUID sessionId) {
		if (server == null) {
			return CompletableFuture.completedFuture(TeleportStatus.SERVER_UNAVAILABLE);
		}
		if (sessionId == null) {
			return CompletableFuture.completedFuture(TeleportStatus.TARGET_UNAVAILABLE);
		}

		Tpa.Session session = sessions.get(sessionId);
		if (session == null) {
			return CompletableFuture.completedFuture(TeleportStatus.TARGET_UNAVAILABLE);
		}
		if (session.isExpired(Util.getMillis())) {
			remove(sessionId);
			return CompletableFuture.completedFuture(TeleportStatus.TARGET_UNAVAILABLE);
		}

		UUID playerToMoveUuid = session.type() == Tpa.Type.TPA ? session.sender() : session.target();
		UUID destinationPlayerUuid = session.type() == Tpa.Type.TPA ? session.target() : session.sender();
		ServerPlayer playerToMove = server.getPlayerList().getPlayer(playerToMoveUuid);
		ServerPlayer destinationPlayer = server.getPlayerList().getPlayer(destinationPlayerUuid);
		if (playerToMove == null || destinationPlayer == null) {
			remove(sessionId);
			return CompletableFuture.completedFuture(TeleportStatus.PLAYER_DISCONNECTED);
		}
		if (playerToMove.isDeadOrDying() || destinationPlayer.isDeadOrDying()) {
			remove(sessionId);
			return CompletableFuture.completedFuture(TeleportStatus.CANCELLED_BY_EVENT);
		}

		long remainingCooldown = operationManager.getRemainingCooldownMillis(playerToMoveUuid, session.cooldownMillis());
		if (remainingCooldown > 0L) {
			return CompletableFuture.completedFuture(TeleportStatus.COOLDOWN);
		}

		TeleportOperationManager.OperationCreateResult<TpaTeleportPending> createResult = operationManager.createOperation(playerToMoveUuid,
				currentTick, (sequence, tick) -> TpaTeleportPending.fromSession(session, sequence, tick));
		createResult.replaced().ifPresent(this::releaseReplacedTargetPreload);
		acceptedQueue.addLast(new PendingRef(playerToMoveUuid, createResult.pending().pendingSequence()));
		remove(sessionId);
		return createResult.pending().resultFuture();
	}

	public void remove(UUID sessionId) {
		if (sessionId == null) {
			return;
		}
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

	public void tick(MinecraftServer server) {
		if (server == null) {
			return;
		}
		currentTick++;
		advancePending(server);
		cleanupExpiredSessions();
	}

	public void onPlayerQuit(UUID playerUuid) {
		if (playerUuid == null) {
			return;
		}

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
					if (targetSet.isEmpty()) {
						targetIncoming.remove(session.target());
					}
				}
				iterator.remove();
			}
		}

		for (TpaTeleportPending pending : operationManager.currentOperations(TpaTeleportPending.class)) {
			if (pending.playerUuid().equals(playerUuid) || pending.destinationPlayerUuid().equals(playerUuid)) {
				executor.finishOperation(pending, TeleportStatus.CANCELLED);
			}
		}
		acceptedQueue.removeIf(ref -> ref.playerUuid().equals(playerUuid));
	}

	public void clear() {
		sessions.clear();
		targetIncoming.clear();
		acceptedQueue.clear();
		for (TpaTeleportPending pending : operationManager.currentOperations(TpaTeleportPending.class)) {
			executor.finishOperation(pending, TeleportStatus.CANCELLED);
		}
	}

	private void advancePending(MinecraftServer server) {
		int executed = 0;
		while (executed < TPA_BATCH_LIMIT && !acceptedQueue.isEmpty()) {
			PendingRef ref = acceptedQueue.peekFirst();
			Optional<TpaTeleportPending> pending = operationManager.getCurrentOperation(ref.playerUuid(), TpaTeleportPending.class)
					.filter(operation -> operation.pendingSequence() == ref.pendingSequence());
			if (pending.isEmpty()) {
				acceptedQueue.pollFirst();
				continue;
			}
			if (!pending.get().isDelayDone(currentTick)) {
				break;
			}

			acceptedQueue.pollFirst();
			executePending(server, pending.get());
			executed++;
		}
	}

	private void executePending(MinecraftServer server, TpaTeleportPending pending) {
		if (!operationManager.isCurrent(pending)) {
			pending.resultFuture().complete(TeleportStatus.CANCELLED);
			return;
		}

		ServerPlayer destinationPlayer = server.getPlayerList().getPlayer(pending.destinationPlayerUuid());
		if (destinationPlayer == null) {
			executor.finishOperation(pending, TeleportStatus.PLAYER_DISCONNECTED);
			return;
		}
		if (destinationPlayer.isDeadOrDying()) {
			executor.finishOperation(pending, TeleportStatus.CANCELLED_BY_EVENT);
			return;
		}

		TeleportTarget target = TeleportTarget.of((ServerLevel) destinationPlayer.level(), destinationPlayer.position());
		executor.executeResolved(server, pending, target);
	}

	private void cleanupExpiredSessions() {
		long now = Util.getMillis();
		Iterator<Map.Entry<UUID, Tpa.Session>> iterator = sessions.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Tpa.Session> entry = iterator.next();
			Tpa.Session session = entry.getValue();
			if (session.isExpired(now)) {
				LinkedHashSet<UUID> targetSet = targetIncoming.get(session.target());
				if (targetSet != null) {
					targetSet.remove(session.sessionId());
					if (targetSet.isEmpty()) {
						targetIncoming.remove(session.target());
					}
				}
				iterator.remove();
			}
		}
	}

	private record PendingRef(UUID playerUuid, long pendingSequence) {
	}

	private void releaseReplacedTargetPreload(TeleportOperation replaced) {
		preloadManager.release(replaced.playerUuid(), replaced.pendingSequence());
	}
}
