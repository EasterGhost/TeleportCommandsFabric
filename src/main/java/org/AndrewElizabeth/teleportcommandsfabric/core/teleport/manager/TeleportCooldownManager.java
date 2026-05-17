package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager;
 
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportPending;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportRequest;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportServiceSettings;

import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
 
public final class TeleportCooldownManager {
	private long pendingSequence = 0L;
	private final HashMap<UUID, PlayerTeleportState> states = new HashMap<>();
	private final Set<UUID> activePendingPlayers = new HashSet<>();
 
	public PendingCreateResult createPending(UUID playerUuid, TeleportRequest request, long currentTick) {
		PlayerTeleportState state = states.computeIfAbsent(playerUuid, ignored -> new PlayerTeleportState());
		TeleportPending replaced;
		TeleportPending pending = new TeleportPending(playerUuid, ++pendingSequence, request, currentTick);
 
		replaced = state.pending;
		state.pending = pending;
		state.cleanupTimeMillis = 0L;
		activePendingPlayers.add(playerUuid);
 
		if (replaced != null) {
			replaced.resultFuture().complete(TeleportStatus.CANCELLED);
		}
 
		return new PendingCreateResult(pending, Optional.ofNullable(replaced));
	}
 
	public Optional<TeleportPending> getCurrentPending(UUID playerUuid) {
		PlayerTeleportState state = states.get(playerUuid);
		if (state == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(state.pending);
	}
 
	public List<TeleportPending> currentPendings() {
		List<TeleportPending> pendings = new ArrayList<>();
		Iterator<UUID> it = activePendingPlayers.iterator();
		while (it.hasNext()) {
			UUID playerUuid = it.next();
			PlayerTeleportState state = states.get(playerUuid);
			if (state == null || state.pending == null) {
				it.remove();
				continue;
			}
			pendings.add(state.pending);
		}
		return pendings;
	}
 
	public boolean hasCurrentPendings() {
		return !activePendingPlayers.isEmpty();
	}
 
	public int visitCurrentPendings(Predicate<TeleportPending> visitor) {
		int visited = 0;
		Iterator<UUID> it = activePendingPlayers.iterator();
		while (it.hasNext()) {
			UUID playerUuid = it.next();
			PlayerTeleportState state = states.get(playerUuid);
			if (state == null || state.pending == null) {
				it.remove();
				continue;
			}
 
			visited++;
			if (!visitor.test(state.pending)) {
				break;
			}
		}
		return visited;
	}
 
	public boolean isCurrent(UUID playerUuid, long pendingSequence) {
		PlayerTeleportState state = states.get(playerUuid);
		if (state == null) {
			return false;
		}
		return state.pending != null && state.pending.pendingSequence() == pendingSequence;
	}
 
	public boolean cancelPending(UUID playerUuid, long pendingSequence, TeleportStatus status) {
		PlayerTeleportState state = states.get(playerUuid);
		if (state == null) {
			return false;
		}
 
		TeleportPending pending = state.pending;
		if (pending == null || pending.pendingSequence() != pendingSequence) {
			return false;
		}
		state.pending = null;
		activePendingPlayers.remove(playerUuid);
 
		pending.resultFuture().complete(status);
		return true;
	}
 
	public boolean markQueuedIfCurrentAndDelayDone(UUID playerUuid, long pendingSequence, long currentTick) {
		PlayerTeleportState state = states.get(playerUuid);
		if (state == null) {
			return false;
		}
 
		if (state.pending == null || state.pending.pendingSequence() != pendingSequence) {
			return false;
		}
		if (!state.pending.isDelayDone(currentTick)) {
			return false;
		}
		state.pending.markQueued();
		return true;
	}
 
	public long getRemainingCooldownMillis(UUID playerUuid, long cooldownMillis) {
		PlayerTeleportState state = states.get(playerUuid);
		if (state == null || cooldownMillis <= 0L) {
			return 0L;
		}
 
		long elapsed = Util.getMillis() - state.lastSuccessTimeMillis;
		return Math.max(0L, cooldownMillis - elapsed);
	}
 
	public void markSuccess(UUID playerUuid, long pendingSequence) {
		PlayerTeleportState state = states.computeIfAbsent(playerUuid, ignored -> new PlayerTeleportState());
 
		if (state.pending != null && state.pending.pendingSequence() == pendingSequence) {
			state.pending = null;
			activePendingPlayers.remove(playerUuid);
		}
		state.lastSuccessTimeMillis = Util.getMillis();
		state.cleanupTimeMillis = 0L;
	}
 
	public void onPlayerJoin(UUID playerUuid) {
		if (playerUuid == null) {
			return;
		}
 
		PlayerTeleportState state = states.get(playerUuid);
		if (state != null) {
			state.cleanupTimeMillis = 0L;
		}
	}
 
	public Optional<TeleportPending> onPlayerQuit(UUID playerUuid, long currentTick) {
		if (playerUuid == null) {
			return Optional.empty();
		}
 
		PlayerTeleportState state = states.computeIfAbsent(playerUuid, ignored -> new PlayerTeleportState());
		TeleportPending pending = state.pending;
		state.pending = null;
		if (pending != null) {
			activePendingPlayers.remove(playerUuid);
		}
		state.cleanupTimeMillis = Util.getMillis() + TeleportServiceSettings.COOLDOWN_CLEANUP_DELAY.toMillis();
 
		if (pending != null) {
			pending.resultFuture().complete(TeleportStatus.CANCELLED);
		}
		return Optional.ofNullable(pending);
	}
 
	public void cleanupExpiredOfflineStates() {
		long now = Util.getMillis();
		states.entrySet().removeIf(entry -> {
			PlayerTeleportState state = entry.getValue();
			return state.pending == null && state.cleanupTimeMillis > 0L && now >= state.cleanupTimeMillis;
		});
	}
 
	public void clear() {
		for (PlayerTeleportState state : states.values()) {
			if (state.pending != null) {
				state.pending.resultFuture().complete(TeleportStatus.CANCELLED);
			}
			state.pending = null;
			state.cleanupTimeMillis = 0L;
		}
		states.clear();
		activePendingPlayers.clear();
		pendingSequence = 0L;
	}
 
	public record PendingCreateResult(TeleportPending pending, Optional<TeleportPending> replaced) {
		public PendingCreateResult {
			replaced = replaced == null ? Optional.empty() : replaced;
		}
	}
 
	private static final class PlayerTeleportState {
		private long lastSuccessTimeMillis;
		private TeleportPending pending;
		private long cleanupTimeMillis;
	}
}
