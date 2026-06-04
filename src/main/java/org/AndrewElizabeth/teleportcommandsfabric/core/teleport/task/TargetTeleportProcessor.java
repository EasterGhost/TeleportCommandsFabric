package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportOperationManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportPreloadManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.TargetTeleportExecution;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class TargetTeleportProcessor {
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
		if (entry.options().safetyEnabled()) {
			Optional<BlockPos> safePos = TeleportSafety.getSafeBlockPos(BlockPos.containing(target.position()), target.world());
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
			if (!entry.options().safetyEnabled()) {
				finishPreparedTeleport(prepared, prepared.target().position());
				continue;
			}

			BlockPos basePos = BlockPos.containing(prepared.target().position());
			CompletableFuture<Optional<BlockPos>> safetyFuture = CompletableFuture.supplyAsync(
					() -> TeleportSafety.getSafeBlockPos(basePos, prepared.target().world()),
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

		if (!preloadManager.isChunkLoaded(target)) {
			preloadManager.preload(entry, currentTick);
			return Preparation.finished(TeleportStatus.ACCEPTED);
		}

		return Preparation.ready(new PreparedExecution(server, entry, target));
	}

	private Optional<BlockPos> joinSafetyCheck(PreparedSafetyCheck safetyCheck) {
		try {
			return safetyCheck.safetyFuture().join();
		} catch (CancellationException | CompletionException exception) {
			ModConstants.LOGGER.warn("Parallel teleport safety check failed; falling back to server thread", exception);
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
}
