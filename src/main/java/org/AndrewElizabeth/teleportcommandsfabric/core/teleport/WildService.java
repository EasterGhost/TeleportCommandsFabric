package org.AndrewElizabeth.teleportcommandsfabric.core.teleport;

import org.AndrewElizabeth.teleportcommandsfabric.core.record.AsyncRecordedLocationSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportOperationManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportPreloadManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportExecutor;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild.WildChunkLoadCoordinator;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild.WildPositionFinder;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportOperation;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.wild.WildRequest;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.wild.WildTeleportPending;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class WildService {
	private static final int MAX_BATCHES_PER_OPERATION = 4;
	private static final long PRELOAD_LEAD_TICKS = 2L;

	private final TeleportOperationManager operationManager;
	private final TeleportPreloadManager preloadManager;
	private final TeleportExecutor executor;
	private final WildChunkLoadCoordinator loadCoordinator;
	private final Map<OperationKey, SearchState> searches = new LinkedHashMap<>();
	private final Map<UUID, OperationKey> playerSearches = new HashMap<>();
	private final WildTickScheduler<OperationKey> scheduler = new WildTickScheduler<>();
	private final ConcurrentLinkedQueue<OperationKey> completedOperations = new ConcurrentLinkedQueue<>();
	private long currentTick;

	public WildService(AsyncRecordedLocationSource recordedSource, TeleportOperationManager operationManager,
			TeleportPreloadManager preloadManager) {
		this(operationManager, preloadManager, new TeleportExecutor(recordedSource, operationManager),
				new WildChunkLoadCoordinator());
	}

	WildService(TeleportOperationManager operationManager, TeleportPreloadManager preloadManager,
			TeleportExecutor executor, WildChunkLoadCoordinator loadCoordinator) {
		this.operationManager = operationManager;
		this.preloadManager = preloadManager;
		this.executor = executor;
		this.loadCoordinator = loadCoordinator;
	}

	public CompletableFuture<TeleportStatus> request(ServerPlayer player, WildRequest request) {
		if (request == null) {
			return CompletableFuture.completedFuture(TeleportStatus.FAILED);
		}
		if (player == null) {
			return CompletableFuture.completedFuture(TeleportStatus.PLAYER_DISCONNECTED);
		}
		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return CompletableFuture.completedFuture(TeleportStatus.SERVER_UNAVAILABLE);
		}
		if (!server.isSameThread()) {
			return CompletableFuture.failedFuture(new IllegalStateException("WildService.request must be called on the server thread"));
		}
		if (player.isDeadOrDying()) {
			return CompletableFuture.completedFuture(TeleportStatus.CANCELLED_BY_EVENT);
		}
		if (player.level().dimensionType().hasCeiling()) {
			return CompletableFuture.completedFuture(TeleportStatus.TARGET_UNAVAILABLE);
		}
		if (hasCurrentRequest(player.getUUID())) {
			return CompletableFuture.completedFuture(TeleportStatus.ACCEPTED);
		}

		long remainingCooldown = operationManager.getRemainingCooldownMillis(player.getUUID(), request.cooldownMillis());
		if (remainingCooldown > 0L) {
			return CompletableFuture.completedFuture(TeleportStatus.COOLDOWN);
		}

		TeleportOperationManager.OperationCreateResult<WildTeleportPending> createResult = operationManager.createOperation(
				player.getUUID(), currentTick,
				(sequence, tick) -> new WildTeleportPending(player.getUUID(), sequence, tick, request,
						player.blockPosition(), player.level().dimension()));
		createResult.replaced().ifPresent(this::releaseReplacedTargetPreload);

		WildTeleportPending pending = createResult.pending();
		OperationKey key = OperationKey.of(pending);
		SearchState state = new SearchState(pending);
		searches.put(key, state);
		playerSearches.put(pending.playerUuid(), key);
		pending.resultFuture().whenComplete((ignored, throwable) -> completedOperations.add(key));
		long firstBatchTick = firstBatchSubmissionTick(pending);
		if (firstBatchTick <= currentTick) {
			queueNextBatch(server, state);
		} else {
			scheduler.schedule(key, firstBatchTick);
		}
		return pending.resultFuture();
	}

	public void tick(MinecraftServer server) {
		if (server == null) {
			return;
		}
		currentTick++;
		cleanupCompletedOperations();
		processScheduledSearches(server);
		loadCoordinator.tick(server, currentTick);
		validateActiveSearches(server);
		processReadyBatches(server);
	}

	public boolean hasCurrentRequest(UUID playerUuid) {
		return operationManager.getCurrentOperation(playerUuid, WildTeleportPending.class).isPresent();
	}

	public void onPlayerQuit(UUID playerUuid) {
		if (playerUuid == null) {
			return;
		}
		OperationKey key = playerSearches.get(playerUuid);
		if (key != null) {
			releaseSearch(key);
		}
	}

	public void onPlayerChangeLevel(UUID playerUuid, ResourceKey<Level> destination) {
		if (playerUuid == null || destination == null) {
			return;
		}
		OperationKey key = playerSearches.get(playerUuid);
		SearchState state = key == null ? null : searches.get(key);
		if (state != null && !state.pending().dimension().equals(destination)) {
			finishSearch(state, TeleportStatus.CANCELLED);
		}
	}

	public void shutdown() {
		for (SearchState state : List.copyOf(searches.values())) {
			if (operationManager.isCurrent(state.pending())) {
				executor.finishOperation(state.pending(), TeleportStatus.CANCELLED);
			}
		}
		searches.clear();
		playerSearches.clear();
		scheduler.clear();
		completedOperations.clear();
		loadCoordinator.clear();
	}

	static long firstBatchSubmissionTick(WildTeleportPending pending) {
		return Math.max(pending.createTick(), pending.delayUntilTick() - PRELOAD_LEAD_TICKS);
	}

	private void processScheduledSearches(MinecraftServer server) {
		for (OperationKey key : scheduler.drainDue(currentTick)) {
			SearchState state = searches.get(key);
			if (state == null) {
				continue;
			}
			if (!operationManager.isCurrent(state.pending())) {
				releaseSearch(key);
				continue;
			}

			TeleportStatus invalidStatus = validatePlayer(server, state.pending());
			if (invalidStatus != null) {
				finishSearch(state, invalidStatus);
				continue;
			}

			if (state.deferredBatch() != null) {
				if (!state.pending().isDelayDone(currentTick)) {
					scheduler.schedule(key, state.pending().delayUntilTick());
					continue;
				}
				processReadyBatch(server, state, state.deferredBatch());
			} else if (!state.batchActive()) {
				queueNextBatch(server, state);
			}
		}
	}

	private void processReadyBatches(MinecraftServer server) {
		for (WildChunkLoadCoordinator.ReadyBatch batch : loadCoordinator.drainReadyBatches(
				WildChunkLoadCoordinator.MAX_READY_BATCHES_PER_TICK)) {
			OperationKey key = new OperationKey(batch.playerUuid(), batch.pendingSequence());
			SearchState state = searches.get(key);
			if (state == null) {
				loadCoordinator.releaseOperation(batch.playerUuid(), batch.pendingSequence());
				continue;
			}
			if (!operationManager.isCurrent(state.pending())) {
				releaseSearch(key);
				continue;
			}

			if (!state.pending().isDelayDone(currentTick)) {
				state.deferBatch(batch);
				scheduler.schedule(key, state.pending().delayUntilTick());
				continue;
			}
			processReadyBatch(server, state, batch);
		}
	}

	private void validateActiveSearches(MinecraftServer server) {
		for (WildChunkLoadCoordinator.BatchKey batchKey : loadCoordinator.activeBatchKeys()) {
			OperationKey key = new OperationKey(batchKey.playerUuid(), batchKey.pendingSequence());
			SearchState state = searches.get(key);
			if (state == null) {
				loadCoordinator.releaseOperation(key.playerUuid(), key.pendingSequence());
				continue;
			}
			if (!operationManager.isCurrent(state.pending())) {
				releaseSearch(key);
				continue;
			}
			TeleportStatus invalidStatus = validatePlayer(server, state.pending());
			if (invalidStatus != null) {
				finishSearch(state, invalidStatus);
			}
		}
	}

	private void processReadyBatch(MinecraftServer server, SearchState state,
			WildChunkLoadCoordinator.ReadyBatch batch) {
		state.clearDeferredBatch();
		state.markBatchFinished();

		ServerLevel world = server.getLevel(batch.dimension());
		if (world == null) {
			finishSearch(state, TeleportStatus.TARGET_UNAVAILABLE);
			return;
		}

		Map<ChunkPos, LevelChunk> chunks = new HashMap<>();
		for (ChunkPos chunkPos : batch.loadedChunks()) {
			LevelChunk chunk = world.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
			if (chunk != null) {
				chunks.put(chunkPos, chunk);
			}
		}

		List<WildPositionFinder.Candidate> candidates = WildPositionFinder.findSafePositions(
				world, state.pending(), chunks);
		if (!candidates.isEmpty()) {
			WildPositionFinder.Candidate selected = candidates.get(state.pending().random().nextInt(candidates.size()));
			loadCoordinator.finishBatch(batch, selected.chunkPos());
			executeTarget(server, state, selected.position());
			return;
		}

		loadCoordinator.finishBatch(batch, null);
		if (state.submittedBatches() >= MAX_BATCHES_PER_OPERATION) {
			finishSearch(state, TeleportStatus.NO_SAFE_POSITION);
		} else {
			scheduler.schedule(OperationKey.of(state.pending()), currentTick + 1L);
		}
	}

	private void queueNextBatch(MinecraftServer server, SearchState state) {
		if (state.batchActive()) {
			return;
		}
		if (state.submittedBatches() >= MAX_BATCHES_PER_OPERATION) {
			finishSearch(state, TeleportStatus.NO_SAFE_POSITION);
			return;
		}

		ServerLevel world = server.getLevel(state.pending().dimension());
		if (world == null || world.dimensionType().hasCeiling()) {
			finishSearch(state, TeleportStatus.TARGET_UNAVAILABLE);
			return;
		}

		List<ChunkPos> chunks = WildPositionFinder.sampleChunkBatch(world, state.pending(), state.usedChunks());
		if (chunks.isEmpty()) {
			finishSearch(state, TeleportStatus.NO_SAFE_POSITION);
			return;
		}
		chunks.forEach(chunk -> state.usedChunks().add(chunk.pack()));
		int batchIndex = state.nextBatchIndex();
		if (!loadCoordinator.submitBatch(state.pending().playerUuid(), state.pending().pendingSequence(),
				batchIndex, state.pending().dimension(), chunks)) {
			finishSearch(state, TeleportStatus.FAILED);
			return;
		}
		state.markBatchSubmitted();
	}

	private void executeTarget(MinecraftServer server, SearchState state, BlockPos target) {
		ServerLevel world = server.getLevel(state.pending().dimension());
		if (world == null) {
			finishSearch(state, TeleportStatus.TARGET_UNAVAILABLE);
			return;
		}
		executor.executeResolved(server, state.pending(), TeleportTarget.centered(world, target));
		releaseSearch(OperationKey.of(state.pending()));
	}

	private void finishSearch(SearchState state, TeleportStatus status) {
		executor.finishOperation(state.pending(), status);
		releaseSearch(OperationKey.of(state.pending()));
	}

	private TeleportStatus validatePlayer(MinecraftServer server, WildTeleportPending pending) {
		ServerPlayer player = server.getPlayerList().getPlayer(pending.playerUuid());
		if (player == null) {
			return TeleportStatus.PLAYER_DISCONNECTED;
		}
		if (player.isDeadOrDying()) {
			return TeleportStatus.CANCELLED_BY_EVENT;
		}
		if (!player.level().dimension().equals(pending.dimension())) {
			return TeleportStatus.CANCELLED;
		}
		return null;
	}

	private void cleanupCompletedOperations() {
		OperationKey key;
		while ((key = completedOperations.poll()) != null) {
			SearchState state = searches.get(key);
			if (state != null && state.pending().resultFuture().isDone()) {
				releaseSearch(key);
			}
		}
	}

	private void releaseSearch(OperationKey key) {
		SearchState removed = searches.remove(key);
		if (removed != null) {
			playerSearches.remove(key.playerUuid(), key);
			scheduler.cancel(key);
			loadCoordinator.releaseOperation(key.playerUuid(), key.pendingSequence());
		}
	}

	private void releaseReplacedTargetPreload(TeleportOperation replaced) {
		preloadManager.release(replaced.playerUuid(), replaced.pendingSequence());
	}

	private record OperationKey(UUID playerUuid, long pendingSequence) {
		private static OperationKey of(WildTeleportPending pending) {
			return new OperationKey(pending.playerUuid(), pending.pendingSequence());
		}
	}

	private static final class SearchState {
		private final WildTeleportPending pending;
		private final Set<Long> usedChunks = new HashSet<>();
		private int submittedBatches;
		private boolean batchActive;
		private WildChunkLoadCoordinator.ReadyBatch deferredBatch;

		private SearchState(WildTeleportPending pending) {
			this.pending = pending;
		}

		private WildTeleportPending pending() { return pending; }
		private Set<Long> usedChunks() { return usedChunks; }
		private int submittedBatches() { return submittedBatches; }
		private int nextBatchIndex() { return submittedBatches; }
		private boolean batchActive() { return batchActive; }
		private WildChunkLoadCoordinator.ReadyBatch deferredBatch() { return deferredBatch; }

		private void markBatchSubmitted() {
			submittedBatches++;
			batchActive = true;
		}

		private void markBatchFinished() {
			batchActive = false;
		}

		private void deferBatch(WildChunkLoadCoordinator.ReadyBatch batch) {
			deferredBatch = batch;
		}

		private void clearDeferredBatch() {
			deferredBatch = null;
		}
	}
}
