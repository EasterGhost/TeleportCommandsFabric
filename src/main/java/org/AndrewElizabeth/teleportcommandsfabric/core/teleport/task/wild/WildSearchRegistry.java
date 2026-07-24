package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.wild.WildTeleportPending;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

final class WildSearchRegistry {
	private final Map<OperationKey, SearchState> searches = new LinkedHashMap<>();
	private final Map<UUID, OperationKey> playerSearches = new HashMap<>();
	private final WildTickScheduler<OperationKey> scheduler = new WildTickScheduler<>();
	private final ConcurrentLinkedQueue<OperationKey> completedOperations = new ConcurrentLinkedQueue<>();

	SearchState register(WildTeleportPending pending) {
		OperationKey key = OperationKey.of(pending);
		SearchState state = new SearchState(pending);
		searches.put(key, state);
		playerSearches.put(pending.playerUuid(), key);
		pending.resultFuture().whenComplete((ignored, throwable) -> completedOperations.add(key));
		return state;
	}

	SearchState get(OperationKey key) {
		return searches.get(key);
	}

	SearchState getByPlayer(UUID playerUuid) {
		OperationKey key = playerSearches.get(playerUuid);
		return key == null ? null : searches.get(key);
	}

	List<SearchState> snapshot() {
		return List.copyOf(searches.values());
	}

	void schedule(OperationKey key, long tick) {
		scheduler.schedule(key, tick);
	}

	List<OperationKey> drainScheduled(long currentTick) {
		return scheduler.drainDue(currentTick);
	}

	List<OperationKey> drainCompleted() {
		List<OperationKey> completed = new ArrayList<>();
		OperationKey key;
		while ((key = completedOperations.poll()) != null) {
			completed.add(key);
		}
		return List.copyOf(completed);
	}

	SearchState remove(OperationKey key) {
		SearchState removed = searches.remove(key);
		if (removed != null) {
			playerSearches.remove(key.playerUuid(), key);
			scheduler.cancel(key);
		}
		return removed;
	}

	void clear() {
		searches.clear();
		playerSearches.clear();
		scheduler.clear();
		completedOperations.clear();
	}

	record OperationKey(UUID playerUuid, long pendingSequence) {
		static OperationKey of(WildTeleportPending pending) {
			return new OperationKey(pending.playerUuid(), pending.pendingSequence());
		}
	}

	static final class SearchState {
		private final WildTeleportPending pending;
		private final Set<Long> usedChunks = new HashSet<>();
		private int submittedBatches;
		private boolean batchActive;
		private WildChunkLoadCoordinator.ReadyBatch deferredBatch;

		private SearchState(WildTeleportPending pending) {
			this.pending = pending;
		}

		WildTeleportPending pending() { return pending; }
		Set<Long> usedChunks() { return usedChunks; }
		int submittedBatches() { return submittedBatches; }
		int nextBatchIndex() { return submittedBatches; }
		boolean batchActive() { return batchActive; }
		WildChunkLoadCoordinator.ReadyBatch deferredBatch() { return deferredBatch; }

		void markBatchSubmitted() {
			submittedBatches++;
			batchActive = true;
		}

		void markBatchFinished() {
			batchActive = false;
		}

		void deferBatch(WildChunkLoadCoordinator.ReadyBatch batch) {
			deferredBatch = batch;
		}

		void clearDeferredBatch() {
			deferredBatch = null;
		}
	}
}
