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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TargetTeleportProcessor {
	private static final long SAFETY_CHECK_TIMEOUT_MILLIS = 5000L;

	private final TeleportOperationManager operationManager;
	private final TeleportPreloadManager preloadManager;
	private final SafetyThreadPool workerPool;
	private final TeleportExecutor executor;

	public TargetTeleportProcessor(TeleportOperationManager operationManager, TeleportPreloadManager preloadManager, SafetyThreadPool workerPool,
			TeleportExecutor executor) {
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
			Optional<BlockPos> safePos = TeleportSafety.getSafeBlockPos(basePos, target.world(), createBlockStateReader(target.world(), basePos));
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
			SafetyCancellation cancellation = new SafetyCancellation();
			CompletableFuture<Void> terminationFuture = new CompletableFuture<>();
			CompletableFuture<Optional<BlockPos>> safetyFuture = CompletableFuture.supplyAsync(
					() -> {
						try {
							return TeleportSafety.getSafeBlockPos(basePos, prepared.target().world(), reader,
									cancellation::isRequested);
						} finally {
							terminationFuture.complete(null);
						}
					},
					workerPool.getExecutor());
			if (safetyChecks == null) {
				safetyChecks = new ArrayList<>();
			}
			safetyChecks.add(new PreparedSafetyCheck(prepared, basePos, safetyFuture, terminationFuture, cancellation));
		}

		if (safetyChecks == null) {
			return;
		}

		long safetyDeadlineNanos = System.nanoTime()
				+ TimeUnit.MILLISECONDS.toNanos(SAFETY_CHECK_TIMEOUT_MILLIS);
		for (PreparedSafetyCheck safetyCheck : safetyChecks) {
			SafetyCheckResult result = joinSafetyCheck(safetyCheck, safetyDeadlineNanos);
			if (result.timedOut()) {
				finishTimedOutSafetyCheck(safetyCheck);
				continue;
			}
			Optional<BlockPos> safePos = result.safePos();
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
		return LoadedChunkBlockStateReader.create(world, basePos);
	}

	private SafetyCheckResult joinSafetyCheck(PreparedSafetyCheck safetyCheck, long deadlineNanos) {
		try {
			long remainingNanos = Math.max(0L, deadlineNanos - System.nanoTime());
			return SafetyCheckResult.completed(
					safetyCheck.safetyFuture().get(remainingNanos, TimeUnit.NANOSECONDS));
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			ModConstants.LOGGER.warn("Parallel teleport safety check was interrupted; falling back to server thread",
					exception);
			return SafetyCheckResult.completed(
					TeleportSafety.getSafeBlockPos(safetyCheck.basePos(), safetyCheck.prepared().target().world()));
		} catch (CancellationException | ExecutionException exception) {
			ModConstants.LOGGER.warn("Parallel teleport safety check failed; falling back to server thread", exception);
			return SafetyCheckResult.completed(
					TeleportSafety.getSafeBlockPos(safetyCheck.basePos(), safetyCheck.prepared().target().world()));
		} catch (TimeoutException exception) {
			safetyCheck.cancellation().request();
			ModConstants.LOGGER.warn(
					"Parallel teleport safety batch exceeded its {} ms deadline; cancelling the unfinished worker task",
					SAFETY_CHECK_TIMEOUT_MILLIS, exception);
			return SafetyCheckResult.timeout();
		}
	}

	private void finishTimedOutSafetyCheck(PreparedSafetyCheck safetyCheck) {
		TargetTeleportExecution entry = safetyCheck.prepared().entry();
		executor.finishOperation(entry.pending(), TeleportStatus.FAILED);
		safetyCheck.terminationFuture().whenComplete((ignored, throwable) ->
				safetyCheck.prepared().server().execute(() ->
						preloadManager.release(entry.playerUuid(), entry.pendingSequence())));
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
			CompletableFuture<Optional<BlockPos>> safetyFuture,
			CompletableFuture<Void> terminationFuture,
			SafetyCancellation cancellation) {
	}

	private record SafetyCheckResult(Optional<BlockPos> safePos, boolean timedOut) {
		private static SafetyCheckResult completed(Optional<BlockPos> safePos) {
			return new SafetyCheckResult(safePos, false);
		}

		private static SafetyCheckResult timeout() {
			return new SafetyCheckResult(Optional.empty(), true);
		}
	}

	private static final class SafetyCancellation {
		private final AtomicBoolean requested = new AtomicBoolean();

		private boolean isRequested() {
			return requested.get();
		}

		private void request() {
			requested.set(true);
		}
	}
}
