package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportOperationManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportExecutor;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild.WildSearchRegistry.OperationKey;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild.WildSearchRegistry.SearchState;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class WildExecutionProcessor {
	private static final int MAX_BATCHES_PER_OPERATION = 4;
	private static final long PRELOAD_LEAD_TICKS = 2L;

	private final TeleportOperationManager operationManager;
	private final TeleportExecutor executor;
	private final WildChunkLoadCoordinator loadCoordinator;
	private final WildSearchRegistry searchRegistry = new WildSearchRegistry();
	private long currentTick;

	public WildExecutionProcessor(TeleportOperationManager operationManager, TeleportExecutor executor) {
		this(operationManager, executor, new WildChunkLoadCoordinator());
	}

	WildExecutionProcessor(TeleportOperationManager operationManager, TeleportExecutor executor,
			WildChunkLoadCoordinator loadCoordinator) {
		this.operationManager = Objects.requireNonNull(operationManager, "operationManager");
		this.executor = Objects.requireNonNull(executor, "executor");
		this.loadCoordinator = Objects.requireNonNull(loadCoordinator, "loadCoordinator");
	}

	public void start(MinecraftServer server, WildTeleportPending pending, long currentTick) {
		Objects.requireNonNull(server, "server");
		Objects.requireNonNull(pending, "pending");
		this.currentTick = currentTick;

		OperationKey key = OperationKey.of(pending);
		SearchState state = searchRegistry.register(pending);

		long firstBatchTick = firstBatchSubmissionTick(pending);
		if (firstBatchTick <= currentTick) {
			queueNextBatch(server, state);
		} else {
			searchRegistry.schedule(key, firstBatchTick);
		}
	}

	public void tick(MinecraftServer server, long currentTick) {
		if (server == null) {
			return;
		}
		this.currentTick = currentTick;
		cleanupCompletedOperations();
		processScheduledSearches(server);
		loadCoordinator.tick(server, currentTick);
		validateActiveSearches(server);
		processReadyBatches(server);
	}

	public void onPlayerQuit(UUID playerUuid) {
		if (playerUuid == null) {
			return;
		}
		SearchState state = searchRegistry.getByPlayer(playerUuid);
		if (state != null) {
			releaseSearch(OperationKey.of(state.pending()));
		}
	}

	public void onPlayerChangeLevel(UUID playerUuid, ResourceKey<Level> destination) {
		if (playerUuid == null || destination == null) {
			return;
		}
		SearchState state = searchRegistry.getByPlayer(playerUuid);
		if (state != null && !state.pending().dimension().equals(destination)) {
			finishSearch(state, TeleportStatus.CANCELLED);
		}
	}

	public void shutdown() {
		for (SearchState state : searchRegistry.snapshot()) {
			if (operationManager.isCurrent(state.pending())) {
				executor.finishOperation(state.pending(), TeleportStatus.CANCELLED);
			}
		}
		searchRegistry.clear();
		loadCoordinator.clear();
	}

	static long firstBatchSubmissionTick(WildTeleportPending pending) {
		return Math.max(pending.createTick(), pending.delayUntilTick() - PRELOAD_LEAD_TICKS);
	}

	private void processScheduledSearches(MinecraftServer server) {
		for (OperationKey key : searchRegistry.drainScheduled(currentTick)) {
			SearchState state = searchRegistry.get(key);
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
					searchRegistry.schedule(key, state.pending().delayUntilTick());
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
			SearchState state = searchRegistry.get(key);
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
				searchRegistry.schedule(key, state.pending().delayUntilTick());
				continue;
			}
			processReadyBatch(server, state, batch);
		}
	}

	private void validateActiveSearches(MinecraftServer server) {
		for (WildChunkLoadCoordinator.BatchKey batchKey : loadCoordinator.activeBatchKeys()) {
			OperationKey key = new OperationKey(batchKey.playerUuid(), batchKey.pendingSequence());
			SearchState state = searchRegistry.get(key);
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
			LevelChunk chunk = world.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
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
			searchRegistry.schedule(OperationKey.of(state.pending()), currentTick + 1L);
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
		chunks.forEach(chunk -> state.usedChunks().add(chunk.toLong()));
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
		for (OperationKey key : searchRegistry.drainCompleted()) {
			SearchState state = searchRegistry.get(key);
			if (state != null && state.pending().resultFuture().isDone()) {
				releaseSearch(key);
			}
		}
	}

	private void releaseSearch(OperationKey key) {
		SearchState removed = searchRegistry.remove(key);
		if (removed != null) {
			loadCoordinator.releaseOperation(key.playerUuid(), key.pendingSequence());
		}
	}
}
