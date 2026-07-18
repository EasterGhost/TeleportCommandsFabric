package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkResult;
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
import java.util.concurrent.ConcurrentLinkedQueue;

public final class WildChunkLoadCoordinator {
	public static final int MAX_ACTIVE_CHUNKS = 32;
	public static final int MAX_ACTIVE_BATCHES = 8;
	public static final int MAX_READY_BATCHES_PER_TICK = 8;
	public static final long LOAD_TIMEOUT_TICKS = 600L;
	private static final int TICKET_RADIUS = 0;

	private final ArrayDeque<QueuedBatch> waitingBatches = new ArrayDeque<>();
	private final Set<BatchKey> trackedBatches = new HashSet<>();
	private final Map<BatchKey, BatchState> activeBatches = new LinkedHashMap<>();
	private final Map<ChunkKey, ChunkLoad> chunkLoads = new HashMap<>();
	private final Map<OperationKey, RetainedChunk> retainedChunks = new HashMap<>();
	private final ConcurrentLinkedQueue<LoadCompletion> completionQueue = new ConcurrentLinkedQueue<>();
	private final ArrayDeque<ReadyBatch> readyBatches = new ArrayDeque<>();
	private long nextLoadSequence;
	private int activeTicketCount;

	public boolean submitBatch(UUID playerUuid, long pendingSequence, int batchIndex,
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

	public void tick(MinecraftServer server, long currentTick) {
		consumeCompletions();
		expireLoads(currentTick);
		promoteReadyBatches();
		admitWaitingBatches(server, currentTick);
		promoteReadyBatches();
	}

	public List<ReadyBatch> drainReadyBatches(int limit) {
		if (limit <= 0 || readyBatches.isEmpty()) {
			return List.of();
		}
		List<ReadyBatch> drained = new ArrayList<>(Math.min(limit, readyBatches.size()));
		while (drained.size() < limit && !readyBatches.isEmpty()) {
			drained.add(readyBatches.pollFirst());
		}
		return List.copyOf(drained);
	}

	public void finishBatch(ReadyBatch readyBatch, ChunkPos retainedChunk) {
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
				releaseConsumer(state.key(), chunkKey);
			}
		}
	}

	public void releaseOperation(UUID playerUuid, long pendingSequence) {
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
				releaseConsumer(batch.key(), chunkKey);
			}
		}

		RetainedChunk retained = retainedChunks.remove(operationKey);
		if (retained != null) {
			releaseConsumer(retained.owner(), retained.chunkKey());
		}
	}

	public void clear() {
		for (ChunkLoad load : List.copyOf(chunkLoads.values())) {
			releaseTicket(load);
		}
		waitingBatches.clear();
		trackedBatches.clear();
		activeBatches.clear();
		chunkLoads.clear();
		retainedChunks.clear();
		completionQueue.clear();
		readyBatches.clear();
		activeTicketCount = 0;
	}

	public int activeTicketCount() {
		return activeTicketCount;
	}

	public int activeBatchCount() {
		return activeBatches.size();
	}

	public List<BatchKey> activeBatchKeys() {
		return List.copyOf(activeBatches.keySet());
	}

	public int waitingBatchCount() {
		return waitingBatches.size();
	}

	private void consumeCompletions() {
		LoadCompletion completion;
		while ((completion = completionQueue.poll()) != null) {
			ChunkLoad load = chunkLoads.get(completion.key());
			if (load == null || load.loadSequence() != completion.loadSequence()
					|| load.state() != LoadState.PENDING) {
				continue;
			}
			completeLoad(load, completion.success() ? LoadState.SUCCESS : LoadState.FAILED);
		}
	}

	private void expireLoads(long currentTick) {
		for (ChunkLoad load : List.copyOf(chunkLoads.values())) {
			if (load.state() == LoadState.PENDING && currentTick - load.startTick() >= LOAD_TIMEOUT_TICKS) {
				completeLoad(load, LoadState.FAILED);
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
		long newChunkCount = queued.chunks().stream()
				.map(chunkPos -> new ChunkKey(queued.dimension(), chunkPos))
				.filter(key -> !chunkLoads.containsKey(key))
				.count();
		return activeTicketCount + newChunkCount <= MAX_ACTIVE_CHUNKS;
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
			ChunkLoad load = chunkLoads.get(chunkKey);
			if (load == null) {
				load = startLoad(level, chunkKey, currentTick);
				chunkLoads.put(chunkKey, load);
			}
			load.consumers().add(batch.key());
			recordLoadState(batch, load);
		}
	}

	private ChunkLoad startLoad(ServerLevel level, ChunkKey key, long currentTick) {
		long loadSequence = ++nextLoadSequence;
		ChunkLoad load = new ChunkLoad(key, level, loadSequence, currentTick);
		load.markTicketHeld();
		activeTicketCount++;
		try {
			level.getChunkSource().addTicketAndLoadWithRadius(WildTicketTypes.wild(), key.chunkPos(), TICKET_RADIUS)
					.whenComplete((result, throwable) -> completionQueue.add(
							new LoadCompletion(key, loadSequence, isSuccessfulLoad(result, throwable))));
		} catch (RuntimeException exception) {
			ModConstants.LOGGER.warn("Failed to start Wild chunk load for {} in {}", key.chunkPos(), key.dimension().identifier(), exception);
			completeLoad(load, LoadState.FAILED);
		}
		return load;
	}

	static boolean isSuccessfulLoad(Object result, Throwable throwable) {
		return throwable == null && result instanceof ChunkResult<?> chunkResult && chunkResult.isSuccess();
	}

	private boolean removeTrackedBatch(BatchKey key, OperationKey operationKey) {
		if (!key.operationKey().equals(operationKey)) {
			return false;
		}
		trackedBatches.remove(key);
		return true;
	}

	private void completeLoad(ChunkLoad load, LoadState state) {
		if (load.state() != LoadState.PENDING) {
			return;
		}
		load.setState(state);
		if (state == LoadState.FAILED) {
			releaseTicket(load);
		}
		for (BatchKey consumer : List.copyOf(load.consumers())) {
			BatchState batch = activeBatches.get(consumer);
			if (batch != null) {
				recordLoadState(batch, load);
			}
		}
	}

	private void recordLoadState(BatchState batch, ChunkLoad load) {
		if (load.state() == LoadState.PENDING) {
			return;
		}
		batch.terminalChunks().add(load.key());
		if (load.state() == LoadState.SUCCESS) {
			batch.successfulChunks().add(load.key());
		}
	}

	private void releaseConsumer(BatchKey owner, ChunkKey chunkKey) {
		ChunkLoad load = chunkLoads.get(chunkKey);
		if (load == null) {
			return;
		}
		load.consumers().remove(owner);
		if (!load.consumers().isEmpty()) {
			return;
		}
		releaseTicket(load);
		chunkLoads.remove(chunkKey, load);
	}

	private void releaseTicket(ChunkLoad load) {
		if (!load.ticketHeld()) {
			return;
		}
		load.clearTicketHeld();
		activeTicketCount--;
		try {
			load.level().getChunkSource().removeTicketWithRadius(WildTicketTypes.wild(), load.key().chunkPos(), TICKET_RADIUS);
		} catch (RuntimeException exception) {
			ModConstants.LOGGER.warn("Failed to release Wild chunk ticket for {} in {}", load.key().chunkPos(),
					load.key().dimension().identifier(), exception);
		}
	}

	public record ReadyBatch(BatchKey key, ResourceKey<Level> dimension, List<ChunkPos> loadedChunks) {
		public ReadyBatch {
			loadedChunks = loadedChunks == null ? List.of() : List.copyOf(loadedChunks);
		}

		public UUID playerUuid() { return key.playerUuid(); }
		public long pendingSequence() { return key.pendingSequence(); }
		public int batchIndex() { return key.batchIndex(); }
	}

	public record BatchKey(UUID playerUuid, long pendingSequence, int batchIndex) {
		public BatchKey {
			Objects.requireNonNull(playerUuid, "playerUuid");
		}

		OperationKey operationKey() {
			return new OperationKey(playerUuid, pendingSequence);
		}
	}

	private record OperationKey(UUID playerUuid, long pendingSequence) {
	}

	private record ChunkKey(ResourceKey<Level> dimension, ChunkPos chunkPos) {
		private ChunkKey {
			Objects.requireNonNull(dimension, "dimension");
			Objects.requireNonNull(chunkPos, "chunkPos");
		}
	}

	private record QueuedBatch(BatchKey key, ResourceKey<Level> dimension, List<ChunkPos> chunks) {
		private QueuedBatch {
			chunks = List.copyOf(chunks);
		}
	}

	private record RetainedChunk(BatchKey owner, ChunkKey chunkKey) {
	}

	private record LoadCompletion(ChunkKey key, long loadSequence, boolean success) {
	}

	private enum LoadState {
		PENDING,
		SUCCESS,
		FAILED
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

		private BatchKey key() { return key; }
		private ResourceKey<Level> dimension() { return dimension; }
		private List<ChunkKey> chunks() { return chunks; }
		private Set<ChunkKey> terminalChunks() { return terminalChunks; }
		private Set<ChunkKey> successfulChunks() { return successfulChunks; }
		private boolean readyQueued() { return readyQueued; }
		private void markReadyQueued() { readyQueued = true; }
	}

	private static final class ChunkLoad {
		private final ChunkKey key;
		private final ServerLevel level;
		private final long loadSequence;
		private final long startTick;
		private final Set<BatchKey> consumers = new HashSet<>();
		private LoadState state = LoadState.PENDING;
		private boolean ticketHeld;

		private ChunkLoad(ChunkKey key, ServerLevel level, long loadSequence, long startTick) {
			this.key = key;
			this.level = level;
			this.loadSequence = loadSequence;
			this.startTick = startTick;
		}

		private ChunkKey key() { return key; }
		private ServerLevel level() { return level; }
		private long loadSequence() { return loadSequence; }
		private long startTick() { return startTick; }
		private Set<BatchKey> consumers() { return consumers; }
		private LoadState state() { return state; }
		private void setState(LoadState state) { this.state = state; }
		private boolean ticketHeld() { return ticketHeld; }
		private void markTicketHeld() { ticketHeld = true; }
		private void clearTicketHeld() { ticketHeld = false; }
	}
}
