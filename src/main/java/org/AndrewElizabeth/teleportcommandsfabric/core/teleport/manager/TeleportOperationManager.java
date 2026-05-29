package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportServiceSettings;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TargetTeleportPending;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportOperation;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportRequest;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;

import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class TeleportOperationManager {
	private long pendingSequence = 0L;
	private final HashMap<UUID, PlayerTeleportState> states = new HashMap<>();
	private final Set<UUID> activePendingPlayers = new HashSet<>();

	public PendingCreateResult createPending(UUID playerUuid, TeleportRequest request, long currentTick) {
		OperationCreateResult<TargetTeleportPending> result = createOperation(playerUuid, currentTick,
				(sequence, tick) -> new TargetTeleportPending(playerUuid, sequence, request, tick));
		return new PendingCreateResult(result.pending(), result.replaced());
	}

	public <T extends TeleportOperation> OperationCreateResult<T> createOperation(UUID playerUuid, long currentTick,
			OperationFactory<T> factory) {
		Objects.requireNonNull(playerUuid, "playerUuid");
		Objects.requireNonNull(factory, "factory");

		PlayerTeleportState state = states.computeIfAbsent(playerUuid, ignored -> new PlayerTeleportState());
		T pending = Objects.requireNonNull(factory.create(++pendingSequence, currentTick), "pending");
		if (!playerUuid.equals(pending.playerUuid())) {
			throw new IllegalArgumentException("operation player UUID must match requested player UUID");
		}

		TeleportOperation replaced = state.pending;
		state.pending = pending;
		state.cleanupTimeMillis = 0L;
		activePendingPlayers.add(playerUuid);

		if (replaced != null) {
			replaced.resultFuture().complete(TeleportStatus.CANCELLED);
		}

		return new OperationCreateResult<>(pending, Optional.ofNullable(replaced));
	}

	public Optional<TeleportOperation> getCurrentOperation(UUID playerUuid) {
		PlayerTeleportState state = states.get(playerUuid);
		if (state == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(state.pending);
	}

	public <T extends TeleportOperation> Optional<T> getCurrentOperation(UUID playerUuid, Class<T> operationType) {
		Objects.requireNonNull(operationType, "operationType");
		return getCurrentOperation(playerUuid)
				.filter(operationType::isInstance)
				.map(operationType::cast);
	}

	public List<TeleportOperation> currentOperations() {
		return currentOperations(TeleportOperation.class);
	}

	public List<TargetTeleportPending> currentTargetPendings() {
		return currentOperations(TargetTeleportPending.class);
	}

	public <T extends TeleportOperation> List<T> currentOperations(Class<T> operationType) {
		Objects.requireNonNull(operationType, "operationType");
		List<T> operations = new ArrayList<>();
		Iterator<UUID> it = activePendingPlayers.iterator();
		while (it.hasNext()) {
			UUID playerUuid = it.next();
			PlayerTeleportState state = states.get(playerUuid);
			if (state == null || state.pending == null) {
				it.remove();
				continue;
			}
			if (operationType.isInstance(state.pending)) {
				operations.add(operationType.cast(state.pending));
			}
		}
		return operations;
	}

	public boolean hasCurrentOperations() {
		return !activePendingPlayers.isEmpty();
	}

	public int visitCurrentTargetPendings(Predicate<TargetTeleportPending> visitor) {
		return visitCurrentOperations(TargetTeleportPending.class, visitor);
	}

	public <T extends TeleportOperation> int visitCurrentOperations(Class<T> operationType, Predicate<T> visitor) {
		Objects.requireNonNull(operationType, "operationType");
		Objects.requireNonNull(visitor, "visitor");
		int visited = 0;
		Iterator<UUID> it = activePendingPlayers.iterator();
		while (it.hasNext()) {
			UUID playerUuid = it.next();
			PlayerTeleportState state = states.get(playerUuid);
			if (state == null || state.pending == null) {
				it.remove();
				continue;
			}
			if (!operationType.isInstance(state.pending)) {
				continue;
			}

			visited++;
			if (!visitor.test(operationType.cast(state.pending))) {
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

	public boolean isCurrent(TeleportOperation operation) {
		return operation != null && isCurrent(operation.playerUuid(), operation.pendingSequence());
	}

	public boolean cancelPending(UUID playerUuid, long pendingSequence, TeleportStatus status) {
		PlayerTeleportState state = states.get(playerUuid);
		if (state == null) {
			return false;
		}

		TeleportOperation pending = state.pending;
		if (pending == null || pending.pendingSequence() != pendingSequence) {
			return false;
		}
		state.pending = null;
		activePendingPlayers.remove(playerUuid);

		pending.resultFuture().complete(status);
		return true;
	}

	public boolean markTargetQueuedIfCurrentAndDelayDone(UUID playerUuid, long pendingSequence, long currentTick) {
		PlayerTeleportState state = states.get(playerUuid);
		if (state == null) {
			return false;
		}

		if (!(state.pending instanceof TargetTeleportPending pending) || pending.pendingSequence() != pendingSequence) {
			return false;
		}
		if (!pending.isDelayDone(currentTick)) {
			return false;
		}
		pending.markQueued();
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

	public Optional<TeleportOperation> onPlayerQuit(UUID playerUuid, long currentTick) {
		if (playerUuid == null) {
			return Optional.empty();
		}

		PlayerTeleportState state = states.computeIfAbsent(playerUuid, ignored -> new PlayerTeleportState());
		TeleportOperation pending = state.pending;
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

	@FunctionalInterface
	public interface OperationFactory<T extends TeleportOperation> {
		T create(long pendingSequence, long currentTick);
	}

	public record OperationCreateResult<T extends TeleportOperation>(T pending, Optional<TeleportOperation> replaced) {
		public OperationCreateResult {
			replaced = replaced == null ? Optional.empty() : replaced;
		}
	}

	public record PendingCreateResult(TargetTeleportPending pending, Optional<TeleportOperation> replaced) {
		public PendingCreateResult {
			replaced = replaced == null ? Optional.empty() : replaced;
		}
	}

	private static final class PlayerTeleportState {
		private long lastSuccessTimeMillis;
		private TeleportOperation pending;
		private long cleanupTimeMillis;
	}
}
