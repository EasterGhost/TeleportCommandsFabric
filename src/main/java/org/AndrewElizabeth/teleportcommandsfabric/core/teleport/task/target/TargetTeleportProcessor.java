package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.target;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportOperationManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportPreloadManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportExecutor;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.TargetTeleportExecution;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class TargetTeleportProcessor {
	private static final long SAFETY_CHECK_TIMEOUT_MILLIS = 5000L;

	private final TeleportOperationManager operationManager;
	private final TeleportPreloadManager preloadManager;
	private final SafetyThreadPool workerPool;
	private final TeleportExecutor executor;

	public TargetTeleportProcessor(TeleportOperationManager operationManager, TeleportPreloadManager preloadManager,
			SafetyThreadPool workerPool, TeleportExecutor executor) {
		this.operationManager = operationManager;
		this.preloadManager = preloadManager;
		this.workerPool = workerPool;
		this.executor = executor;
	}

	public TeleportStatus executeOne(MinecraftServer server, TargetTeleportExecution entry, long currentTick) {
		Preparation preparation = prepareExecution(server, entry, currentTick);
		if (!preparation.ready()) {
			return preparation.status();
		}

		PreparedExecution prepared = preparation.execution();
		TeleportTarget target = prepared.target();
		if (shouldCheckSafety(entry)) {
			BlockPos basePos = BlockPos.containing(target.position());
			Optional<BlockPos> safePos = TeleportSafety.getSafeBlockPos(basePos, target.world(),
					createBlockStateReader(target.world(), basePos));
			if (safePos.isEmpty()) {
				return finishEntry(entry, TeleportStatus.NO_SAFE_POSITION);
			}
			BlockPos pos = safePos.get();
			target = target.withPosition(new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
		}

		return finishResolvedTeleport(server, entry, target);
	}

	public void executeBatch(MinecraftServer server, List<TargetTeleportExecution> entries, long currentTick) {
		List<PreparedSafetyCheck> safetyChecks = null;
		for (TargetTeleportExecution entry : entries) {
			Preparation preparation = prepareExecution(server, entry, currentTick);
			if (!preparation.ready()) {
				continue;
			}
			PreparedExecution prepared = preparation.execution();
			if (!shouldCheckSafety(entry)) {
				finishPreparedTeleport(prepared, prepared.target().position());
				continue;
			}

			BlockPos basePos = BlockPos.containing(prepared.target().position());
			TeleportSafety.BlockStateReader reader = createBlockStateReader(prepared.target().world(), basePos);
			CompletableFuture<Optional<BlockPos>> safetyFuture = CompletableFuture.supplyAsync(
					() -> TeleportSafety.getSafeBlockPos(basePos, prepared.target().world(), reader),
					workerPool.getExecutor());
			if (safetyChecks == null) {
				safetyChecks = new ArrayList<>();
			}
			safetyChecks.add(new PreparedSafetyCheck(prepared, basePos, safetyFuture));
		}

		if (safetyChecks == null) {
			return;
		}

		for (PreparedSafetyCheck safetyCheck : safetyChecks) {
			Optional<BlockPos> safePos = joinSafetyCheck(safetyCheck);
			if (safePos.isEmpty()) {
				finishEntry(safetyCheck.prepared().entry(), TeleportStatus.NO_SAFE_POSITION);
				continue;
			}

			BlockPos pos = safePos.get();
			finishPreparedTeleport(safetyCheck.prepared(), new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
		}
	}

	public TeleportStatus finishEntry(TargetTeleportExecution entry, TeleportStatus status) {
		TeleportStatus result = executor.finishOperation(entry.pending(), status);
		preloadManager.release(entry.playerUuid(), entry.pendingSequence());
		return result;
	}

	private Preparation prepareExecution(MinecraftServer server, TargetTeleportExecution entry, long currentTick) {
		if (!operationManager.isCurrent(entry.pending())) {
			return Preparation.finished(finishEntry(entry, TeleportStatus.CANCELLED));
		}

		ServerPlayer player = server.getPlayerList().getPlayer(entry.playerUuid());
		if (player == null) {
			return Preparation.finished(finishEntry(entry, TeleportStatus.PLAYER_DISCONNECTED));
		}
		if (player.isDeadOrDying()) {
			return Preparation.finished(finishEntry(entry, TeleportStatus.CANCELLED_BY_EVENT));
		}

		TeleportTarget target = entry.target();
		if (server.getLevel(target.world().dimension()) == null) {
			return Preparation.finished(finishEntry(entry, TeleportStatus.TARGET_UNAVAILABLE));
		}

		if (preloadManager.preload(entry, currentTick)) {
			return Preparation.finished(TeleportStatus.ACCEPTED);
		}

		return Preparation.ready(new PreparedExecution(server, entry, target));
	}

	private boolean shouldCheckSafety(TargetTeleportExecution entry) {
		return entry.options().safetyEnabled() && preloadManager.isEnabled();
	}

	private TeleportSafety.BlockStateReader createBlockStateReader(ServerLevel world, BlockPos basePos) {
		ChunkBlockStateReader reader = ChunkBlockStateReader.create(world, basePos);
		return reader == null ? world::getBlockState : reader;
	}

	private Optional<BlockPos> joinSafetyCheck(PreparedSafetyCheck safetyCheck) {
		try {
			return safetyCheck.safetyFuture().get(SAFETY_CHECK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			ModConstants.LOGGER.warn("Parallel teleport safety check was interrupted; falling back to server thread",
					exception);
			return TeleportSafety.getSafeBlockPos(safetyCheck.basePos(), safetyCheck.prepared().target().world());
		} catch (CancellationException | ExecutionException exception) {
			ModConstants.LOGGER.warn("Parallel teleport safety check failed; falling back to server thread", exception);
			return TeleportSafety.getSafeBlockPos(safetyCheck.basePos(), safetyCheck.prepared().target().world());
		} catch (TimeoutException exception) {
			ModConstants.LOGGER.warn(
					"Parallel teleport safety check timed out after {} ms; falling back to server thread",
					SAFETY_CHECK_TIMEOUT_MILLIS, exception);
			return TeleportSafety.getSafeBlockPos(safetyCheck.basePos(), safetyCheck.prepared().target().world());
		}
	}

	private TeleportStatus finishPreparedTeleport(PreparedExecution prepared, Vec3 destination) {
		return finishResolvedTeleport(prepared.server(), prepared.entry(), prepared.target().withPosition(destination));
	}

	private TeleportStatus finishResolvedTeleport(MinecraftServer server, TargetTeleportExecution entry,
			TeleportTarget target) {
		TeleportStatus status = executor.executeResolved(server, entry.pending(), target);
		preloadManager.release(entry.playerUuid(), entry.pendingSequence());
		return status;
	}

	private record PreparedExecution(
			MinecraftServer server,
			TargetTeleportExecution entry,
			TeleportTarget target) {
	}

	private record Preparation(
			PreparedExecution execution,
			TeleportStatus status) {
		static Preparation ready(PreparedExecution execution) {
			return new Preparation(execution, TeleportStatus.ACCEPTED);
		}

		static Preparation finished(TeleportStatus status) {
			return new Preparation(null, status);
		}

		boolean ready() {
			return execution != null;
		}
	}

	private record PreparedSafetyCheck(
			PreparedExecution prepared,
			BlockPos basePos,
			CompletableFuture<Optional<BlockPos>> safetyFuture) {
	}

	private record ChunkBlockStateReader(
			ServerLevel world,
			long[] chunkKeys,
			LevelChunk[] chunks) implements TeleportSafety.BlockStateReader {
		private static ChunkBlockStateReader create(ServerLevel world, BlockPos basePos) {
			int minChunkX = (basePos.getX() - TeleportSafety.SEARCH_RADIUS) >> 4;
			int maxChunkX = (basePos.getX() + TeleportSafety.SEARCH_RADIUS) >> 4;
			int minChunkZ = (basePos.getZ() - TeleportSafety.SEARCH_RADIUS) >> 4;
			int maxChunkZ = (basePos.getZ() + TeleportSafety.SEARCH_RADIUS) >> 4;
			int chunkCount = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
			long[] chunkKeys = new long[chunkCount];
			LevelChunk[] chunks = new LevelChunk[chunkCount];
			int index = 0;
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
					LevelChunk chunk = world.getChunkSource().getChunkNow(chunkX, chunkZ);
					if (chunk == null) {
						return null;
					}
					chunkKeys[index] = ChunkPos.pack(chunkX, chunkZ);
					chunks[index] = chunk;
					index++;
				}
			}
			return new ChunkBlockStateReader(world, chunkKeys, chunks);
		}

		@Override
		public BlockState getBlockState(BlockPos pos) {
			long chunkKey = ChunkPos.pack(pos);
			for (int i = 0; i < chunkKeys.length; i++) {
				if (chunkKeys[i] == chunkKey) {
					return chunks[i].getBlockState(pos);
				}
			}
			return world.getBlockState(pos);
		}
	}
}
