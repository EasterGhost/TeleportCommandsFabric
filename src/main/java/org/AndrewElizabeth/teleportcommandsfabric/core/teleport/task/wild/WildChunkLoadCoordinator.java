package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild.WildChunkLoadPool.ChunkKey;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild.WildChunkLoadPool.LoadResult;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild.WildChunkLoadPool.LoadState;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

final class WildChunkLoadCoordinator {
	private static final int MAX_ACTIVE_BATCHES = 8;
	static final int MAX_READY_BATCHES_PER_TICK = 8;

	private final ArrayDeque<QueuedBatch> waitingBatches = new ArrayDeque<>();
	private final Set<BatchKey> trackedBatches = new HashSet<>();
	private final Map<BatchKey, BatchState> activeBatches = new LinkedHashMap<>();
	private final Map<OperationKey, RetainedChunk> retainedChunks = new HashMap<>();
	private final ArrayDeque<ReadyBatch> readyBatches = new ArrayDeque<>();
	private final WildChunkLoadPool<BatchKey> loadPool = new WildChunkLoadPool<>();

	boolean submitBatch(UUID playerUuid, long pendingSequence, int batchIndex,
			ResourceKey<Level> dimension, List<ChunkPos> chunks) {
		BatchKey key = new BatchKey(playerUuid, pendingSequence, batchIndex);
		List<ChunkPos> uniqueChunks = chunks == null ? List.of() : chunks.stream().distinct().toList();
		if (uniqueChunks.isEmpty() || uniqueChunks.size() > WildPositionFinder.CHUNKS_PER_BATCH) {
			return false;
		}
		ResourceKey<Level> safeDimension = Objects.requireNonNull(dimension, "dimension");
		if (!trackedBatches.add(key)) {
			return false;
		}
		waitingBatches.addLast(new QueuedBatch(key, safeDimension, uniqueChunks));
		return true;
	}

	void tick(MinecraftServer server, long currentTick) {
		applyLoadResults(loadPool.advance(currentTick));
		promoteReadyBatches();
		admitWaitingBatches(server, currentTick);
		promoteReadyBatches();
	}

	List<ReadyBatch> drainReadyBatches(int limit) {
		if (limit <= 0 || readyBatches.isEmpty()) {
			return List.of();
		}
		List<ReadyBatch> drained = new ArrayList<>(Math.min(limit, readyBatches.size()));
		while (drained.size() < limit && !readyBatches.isEmpty()) {
			drained.add(readyBatches.pollFirst());
		}
		return List.copyOf(drained);
	}

	void finishBatch(ReadyBatch readyBatch, ChunkPos retainedChunk) {
		if (readyBatch == null) {
			return;
		}
		BatchState state = activeBatches.remove(readyBatch.key());
		trackedBatches.remove(readyBatch.key());
		if (state == null) {
			return;
		}

		OperationKey operationKey = state.key().operationKey();
		ChunkKey retainedKey = retainedChunk == null ? null : new ChunkKey(state.dimension(), retainedChunk);
		for (ChunkKey chunkKey : state.chunks()) {
			if (chunkKey.equals(retainedKey) && state.successfulChunks().contains(chunkKey)) {
				retainedChunks.put(operationKey, new RetainedChunk(state.key(), chunkKey));
			} else {
				loadPool.releaseConsumer(state.key(), chunkKey);
			}
		}
	}

	void releaseOperation(UUID playerUuid, long pendingSequence) {
		OperationKey operationKey = new OperationKey(playerUuid, pendingSequence);
		waitingBatches.removeIf(batch -> removeTrackedBatch(batch.key(), operationKey));
		readyBatches.removeIf(batch -> removeTrackedBatch(batch.key(), operationKey));

		List<BatchState> batches = activeBatches.values().stream()
				.filter(batch -> batch.key().operationKey().equals(operationKey))
				.toList();
		for (BatchState batch : batches) {
			activeBatches.remove(batch.key());
			trackedBatches.remove(batch.key());
			for (ChunkKey chunkKey : batch.chunks()) {
				loadPool.releaseConsumer(batch.key(), chunkKey);
			}
		}

		RetainedChunk retained = retainedChunks.remove(operationKey);
		if (retained != null) {
			loadPool.releaseConsumer(retained.owner(), retained.chunkKey());
		}
	}

	void clear() {
		loadPool.clear();
		waitingBatches.clear();
		trackedBatches.clear();
		activeBatches.clear();
		retainedChunks.clear();
		readyBatches.clear();
	}

	List<BatchKey> activeBatchKeys() {
		return List.copyOf(activeBatches.keySet());
	}

	private void applyLoadResults(List<LoadResult<BatchKey>> results) {
		for (LoadResult<BatchKey> result : results) {
			for (BatchKey consumer : result.consumers()) {
				BatchState batch = activeBatches.get(consumer);
				if (batch != null) {
					recordLoadState(batch, result.key(), result.state());
				}
			}
		}
	}

	private void promoteReadyBatches() {
		for (BatchState batch : activeBatches.values()) {
			if (!batch.readyQueued() && batch.terminalChunks().size() == batch.chunks().size()) {
				batch.markReadyQueued();
				List<ChunkPos> loadedChunks = batch.successfulChunks().stream()
						.map(ChunkKey::chunkPos)
						.toList();
				readyBatches.addLast(new ReadyBatch(batch.key(), batch.dimension(), loadedChunks));
			}
		}
	}

	private void admitWaitingBatches(MinecraftServer server, long currentTick) {
		while (!waitingBatches.isEmpty() && activeBatches.size() < MAX_ACTIVE_BATCHES) {
			int candidates = waitingBatches.size();
			boolean admitted = false;
			for (int i = 0; i < candidates; i++) {
				QueuedBatch queued = waitingBatches.pollFirst();
				if (canAdmit(queued)) {
					admitBatch(server, queued, currentTick);
					admitted = true;
					break;
				}
				waitingBatches.addLast(queued);
			}
			if (!admitted) {
				break;
			}
		}
	}

	private boolean canAdmit(QueuedBatch queued) {
		return loadPool.canAcquire(queued.dimension(), queued.chunks());
	}

	private void admitBatch(MinecraftServer server, QueuedBatch queued, long currentTick) {
		List<ChunkKey> chunkKeys = queued.chunks().stream()
				.map(chunkPos -> new ChunkKey(queued.dimension(), chunkPos))
				.toList();
		BatchState batch = new BatchState(queued.key(), queued.dimension(), chunkKeys);
		activeBatches.put(batch.key(), batch);

		ServerLevel level = server.getLevel(queued.dimension());
		if (level == null) {
			batch.terminalChunks().addAll(chunkKeys);
			return;
		}

		for (ChunkKey chunkKey : chunkKeys) {
			LoadState state = loadPool.acquire(level, chunkKey, batch.key(), currentTick);
			recordLoadState(batch, chunkKey, state);
		}
	}

	private boolean removeTrackedBatch(BatchKey key, OperationKey operationKey) {
		if (!key.operationKey().equals(operationKey)) {
			return false;
		}
		trackedBatches.remove(key);
		return true;
	}

	private void recordLoadState(BatchState batch, ChunkKey key, LoadState state) {
		if (state == LoadState.PENDING) {
			return;
		}
		batch.terminalChunks().add(key);
		if (state == LoadState.SUCCESS) {
			batch.successfulChunks().add(key);
		}
	}

	record ReadyBatch(BatchKey key, ResourceKey<Level> dimension, List<ChunkPos> loadedChunks) {
		ReadyBatch {
			loadedChunks = loadedChunks == null ? List.of() : List.copyOf(loadedChunks);
		}

		UUID playerUuid() {
			return key.playerUuid();
		}

		long pendingSequence() {
			return key.pendingSequence();
		}

		int batchIndex() {
			return key.batchIndex();
		}
	}

	record BatchKey(UUID playerUuid, long pendingSequence, int batchIndex) {
		BatchKey {
			Objects.requireNonNull(playerUuid, "playerUuid");
		}

		OperationKey operationKey() {
			return new OperationKey(playerUuid, pendingSequence);
		}
	}

	private record OperationKey(UUID playerUuid, long pendingSequence) {
	}

	private record QueuedBatch(BatchKey key, ResourceKey<Level> dimension, List<ChunkPos> chunks) {
		private QueuedBatch {
			chunks = List.copyOf(chunks);
		}
	}

	private record RetainedChunk(BatchKey owner, ChunkKey chunkKey) {
	}

	private static final class BatchState {
		private final BatchKey key;
		private final ResourceKey<Level> dimension;
		private final List<ChunkKey> chunks;
		private final Set<ChunkKey> terminalChunks = new HashSet<>();
		private final Set<ChunkKey> successfulChunks = new HashSet<>();
		private boolean readyQueued;

		private BatchState(BatchKey key, ResourceKey<Level> dimension, List<ChunkKey> chunks) {
			this.key = key;
			this.dimension = dimension;
			this.chunks = List.copyOf(chunks);
		}

		private BatchKey key() {
			return key;
		}

		private ResourceKey<Level> dimension() {
			return dimension;
		}

		private List<ChunkKey> chunks() {
			return chunks;
		}

		private Set<ChunkKey> terminalChunks() {
			return terminalChunks;
		}

		private Set<ChunkKey> successfulChunks() {
			return successfulChunks;
		}

		private boolean readyQueued() {
			return readyQueued;
		}

		private void markReadyQueued() {
			readyQueued = true;
		}
	}
}
