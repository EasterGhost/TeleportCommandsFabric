package org.AndrewElizabeth.teleportcommandsfabric.core.teleport;

import org.AndrewElizabeth.teleportcommandsfabric.core.record.AsyncRecordedLocationSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportCooldownManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportPreloadManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportBatchDispatcher;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.SafetyThreadPool;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportExecutor;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportPending;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportRequest;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTargetResult;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TeleportService {
	private final TeleportCooldownManager cooldownManager;
	private final TeleportPreloadManager preloadManager;
	private final TeleportBatchDispatcher dispatcher;
	private final SafetyThreadPool workerPool;
	private final TeleportExecutor executor;
	private long currentTick;
	private int admissionRampTick;
	private boolean safetyWarmedUp;

	public TeleportService(AsyncRecordedLocationSource recordedSource) {
		this(recordedSource, new TeleportCooldownManager(), new TeleportPreloadManager(), new TeleportBatchDispatcher(), new SafetyThreadPool());
	}

	public TeleportService(AsyncRecordedLocationSource recordedSource, TeleportCooldownManager cooldownManager,
			TeleportPreloadManager preloadManager, TeleportBatchDispatcher dispatcher, SafetyThreadPool workerPool) {
		this.cooldownManager = cooldownManager;
		this.preloadManager = preloadManager;
		this.dispatcher = dispatcher;
		this.workerPool = workerPool;
		this.executor = new TeleportExecutor(recordedSource, cooldownManager, preloadManager, workerPool);
	}

	public CompletableFuture<TeleportStatus> request(ServerPlayer player, TeleportRequest request) {
		if (player == null || request == null) {
			return CompletableFuture.completedFuture(TeleportStatus.FAILED);
		}

		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return CompletableFuture.completedFuture(TeleportStatus.SERVER_UNAVAILABLE);
		}
		if (!server.isSameThread()) {
			return CompletableFuture.failedFuture(new IllegalStateException("TeleportService.request must be called on the server thread"));
		}
		if (player.isDeadOrDying()) {
			return CompletableFuture.completedFuture(TeleportStatus.CANCELLED_BY_EVENT);
		}

		UUID playerUuid = player.getUUID();
		long remainingCooldown = cooldownManager.getRemainingCooldownMillis(playerUuid, request.options().effectiveCooldownMillis());
		if (remainingCooldown > 0L) {
			return CompletableFuture.completedFuture(TeleportStatus.COOLDOWN);
		}

		TeleportCooldownManager.PendingCreateResult createResult = cooldownManager.createPending(playerUuid, request, currentTick);
		createResult.replaced().ifPresent(replaced -> preloadManager.release(replaced.playerUuid(), replaced.pendingSequence()));

		TeleportPending pending = createResult.pending();
		request.targetFuture().whenComplete((targetResult, throwable) -> {
			if (throwable != null) {
				pending.completeTarget(TeleportTargetResult.failed(TeleportStatus.FAILED));
			} else {
				pending.completeTarget(targetResult);
			}
		});

		return pending.resultFuture();
	}

	public void tick(MinecraftServer server) {
		if (server == null) {
			return;
		}

		if (!safetyWarmedUp) {
			workerPool.warmupSafety(server);
			safetyWarmedUp = true;
		}

		currentTick++;
		dispatcher.beginTick();
		cooldownManager.cleanupExpiredOfflineStates();
		handlePreloadTick();
		advancePending();
		dispatcher.drainBatch(TeleportServiceSettings.SAFETY_BATCH_SIZE, entries -> executor.executeBatch(server, entries, currentTick));
		updateAdmissionRamp();
	}

	public void cancelPending(UUID playerUuid, long pendingSequence, TeleportStatus status) {
		if (cooldownManager.cancelPending(playerUuid, pendingSequence, status)) {
			preloadManager.release(playerUuid, pendingSequence);
		}
	}

	public void onPlayerDeath(UUID playerUuid) {
		cooldownManager.getCurrentPending(playerUuid)
				.ifPresent(pending -> cancelPending(playerUuid, pending.pendingSequence(), TeleportStatus.CANCELLED_BY_EVENT));
	}

	public void onPlayerJoin(UUID playerUuid) {
		cooldownManager.onPlayerJoin(playerUuid);
	}

	public void onPlayerQuit(UUID playerUuid) {
		cooldownManager.onPlayerQuit(playerUuid, currentTick)
				.ifPresent(pending -> preloadManager.release(playerUuid, pending.pendingSequence()));
	}

	public void shutdown() {
		for (TeleportPending pending : cooldownManager.currentPendings()) {
			cancelPending(pending.playerUuid(), pending.pendingSequence(), TeleportStatus.CANCELLED);
		}
		dispatcher.clear();
		preloadManager.releaseAll();
		cooldownManager.clear();
		workerPool.shutdown();
	}

	public int queueSize() {
		return dispatcher.queueSize();
	}

	public int activePreloadTicketCount() {
		return preloadManager.activeTicketCount();
	}

	private void handlePreloadTick() {
		TeleportPreloadManager.PreloadTickResult result = preloadManager.tick(currentTick);
		for (TeleportBatchDispatcher.ExecutionEntry entry : result.ready()) {
			if (cooldownManager.isCurrent(entry.playerUuid(), entry.pendingSequence())) {
				submitReadyExecution(entry);
			}
		}
		for (TeleportBatchDispatcher.ExecutionEntry entry : result.timedOut()) {
			executor.finishEntry(entry, TeleportStatus.FAILED);
		}
	}

	private void advancePending() {
		int admissionLimit = currentReadyAdmissionLimit();
		int[] admitted = { 0 };
		cooldownManager.visitCurrentPendings(pending -> {
			if (admitted[0] >= admissionLimit) {
				return false;
			}
			if (!cooldownManager.isCurrent(pending.playerUuid(), pending.pendingSequence())) {
				return true;
			}
			if (pending.isQueued() || !pending.isTargetDone()) {
				return true;
			}

			TeleportTargetResult targetResult = pending.targetResult();
			if (targetResult instanceof TeleportTargetResult.Failed failed) {
				finishPending(pending, failed.reason());
				admitted[0]++;
				return true;
			}
			if (!(targetResult instanceof TeleportTargetResult.Resolved resolved)) {
				finishPending(pending, TeleportStatus.FAILED);
				admitted[0]++;
				return true;
			}

			TeleportBatchDispatcher.ExecutionEntry entry = toExecutionEntry(pending, resolved.target());
			if (!pending.isDelayDone(currentTick)) {
				if (shouldStartPreloadDuringDelay(pending) && !pending.isPreloadStarted() && !preloadManager.isChunkLoaded(resolved.target())) {
					pending.markPreloadStarted();
					preloadManager.preload(entry, currentTick);
					admitted[0]++;
				}
				return true;
			}

			if (!preloadManager.isChunkLoaded(resolved.target())) {
				pending.markPreloadStarted();
				preloadManager.preload(entry, currentTick);
				admitted[0]++;
				return true;
			}

			submitReadyExecution(entry);
			admitted[0]++;
			return true;
		});
	}

	private int currentReadyAdmissionLimit() {
		if (admissionRampTick == 0) {
			return TeleportServiceSettings.READY_ADMISSION_FIRST_TICK_LIMIT;
		}
		return TeleportServiceSettings.READY_ADMISSION_STEADY_TICK_LIMIT;
	}

	private boolean shouldStartPreloadDuringDelay(TeleportPending pending) {
		return currentTick >= pending.delayUntilTick() - TeleportServiceSettings.PRELOAD_LEAD_TICKS;
	}

	private void updateAdmissionRamp() {
		if (cooldownManager.hasCurrentPendings() || dispatcher.queueSize() > 0 || preloadManager.activeTicketCount() > 0) {
			admissionRampTick++;
		} else {
			admissionRampTick = 0;
		}
	}

	private void submitReadyExecution(TeleportBatchDispatcher.ExecutionEntry entry) {
		if (!cooldownManager.isCurrent(entry.playerUuid(), entry.pendingSequence())) {
			return;
		}

		if (!cooldownManager.markQueuedIfCurrentAndDelayDone(entry.playerUuid(), entry.pendingSequence(), currentTick)) {
			return;
		}
		if (dispatcher.canUseFastPath()) {
			dispatcher.noteFastPathUse();
			executor.executeOne(entry.target().world().getServer(), entry, currentTick);
		} else {
			dispatcher.enqueue(entry);
		}
	}

	private TeleportBatchDispatcher.ExecutionEntry toExecutionEntry(TeleportPending pending, TeleportTarget target) {
		return new TeleportBatchDispatcher.ExecutionEntry(
				pending.playerUuid(),
				pending.pendingSequence(),
				target,
				pending.request().options(),
				pending.resultFuture());
	}

	private void finishPending(TeleportPending pending, TeleportStatus status) {
		cancelPending(pending.playerUuid(), pending.pendingSequence(), status);
	}
}
