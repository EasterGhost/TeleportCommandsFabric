package org.AndrewElizabeth.teleportcommandsfabric.core.teleport;

import org.AndrewElizabeth.teleportcommandsfabric.core.record.AsyncRecordedLocationSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportOperationManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportPreloadManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportBatchDispatcher;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.SafetyThreadPool;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportExecutor;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TargetTeleportProcessor;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportOperation;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.TargetTeleportExecution;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.TargetTeleportPending;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.TeleportRequest;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTargetResult;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;

public final class TeleportService {
	private final TeleportOperationManager operationManager;
	private final TeleportPreloadManager preloadManager;
	private final TeleportBatchDispatcher dispatcher;
	private final SafetyThreadPool workerPool;
	private final TargetTeleportProcessor targetProcessor;
	private long currentTick;
	private int admissionRampTick;
	private boolean safetyWarmedUp;

	public TeleportService(AsyncRecordedLocationSource recordedSource) {
		this(recordedSource, new TeleportOperationManager(), new TeleportPreloadManager(), new TeleportBatchDispatcher(), new SafetyThreadPool());
	}

	public TeleportService(AsyncRecordedLocationSource recordedSource, TeleportOperationManager operationManager,
			TeleportPreloadManager preloadManager, TeleportBatchDispatcher dispatcher, SafetyThreadPool workerPool) {
		this.operationManager = operationManager;
		this.preloadManager = preloadManager;
		this.dispatcher = dispatcher;
		this.workerPool = workerPool;
		TeleportExecutor executor = new TeleportExecutor(recordedSource, operationManager);
		this.targetProcessor = new TargetTeleportProcessor(operationManager, preloadManager, workerPool, executor);
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
		long remainingCooldown = operationManager.getRemainingCooldownMillis(playerUuid, request.options().effectiveCooldownMillis());
		if (remainingCooldown > 0L) {
			return CompletableFuture.completedFuture(TeleportStatus.COOLDOWN);
		}

		TeleportOperationManager.PendingCreateResult createResult = operationManager.createPending(playerUuid, request, currentTick);
		createResult.replaced().ifPresent(replaced -> preloadManager.release(replaced.playerUuid(), replaced.pendingSequence()));

		TargetTeleportPending pending = createResult.pending();
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
		operationManager.cleanupExpiredOfflineStates();
		handlePreloadTick();
		advancePending();
		dispatcher.drainBatch(TeleportServiceSettings.SAFETY_BATCH_SIZE, entries -> targetProcessor.executeBatch(server, entries, currentTick));
		updateAdmissionRamp();
	}

	public void cancelPending(UUID playerUuid, long pendingSequence, TeleportStatus status) {
		if (operationManager.cancelPending(playerUuid, pendingSequence, status)) {
			preloadManager.release(playerUuid, pendingSequence);
		}
	}

	public Optional<TeleportOperation> cancelCurrent(UUID playerUuid, TeleportStatus status) {
		Optional<TeleportOperation> cancelled = operationManager.cancelCurrent(playerUuid, status);
		cancelled.ifPresent(operation -> preloadManager.release(operation.playerUuid(), operation.pendingSequence()));
		return cancelled;
	}

	public void onPlayerDeath(UUID playerUuid) {
		operationManager.getCurrentOperation(playerUuid)
				.ifPresent(pending -> cancelPending(playerUuid, pending.pendingSequence(), TeleportStatus.CANCELLED_BY_EVENT));
	}

	public void onPlayerJoin(UUID playerUuid) {
		operationManager.onPlayerJoin(playerUuid);
	}

	public void onPlayerQuit(UUID playerUuid) {
		operationManager.onPlayerQuit(playerUuid, currentTick)
				.ifPresent(pending -> preloadManager.release(playerUuid, pending.pendingSequence()));
	}

	public void shutdown() {
		for (TargetTeleportPending pending : operationManager.currentTargetPendings()) {
			cancelPending(pending.playerUuid(), pending.pendingSequence(), TeleportStatus.CANCELLED);
		}
		dispatcher.clear();
		preloadManager.releaseAll();
		operationManager.clear();
		workerPool.shutdown();
	}

	public int queueSize() {
		return dispatcher.queueSize();
	}

	public int activePreloadTicketCount() {
		return preloadManager.activeTicketCount();
	}

	public void configurePreload(boolean enabled, int radiusChunks) {
		preloadManager.configure(enabled, radiusChunks);
	}

	private void handlePreloadTick() {
		TeleportPreloadManager.PreloadTickResult result = preloadManager.tick(currentTick);
		for (TargetTeleportExecution entry : result.ready()) {
			if (operationManager.isCurrent(entry.playerUuid(), entry.pendingSequence())) {
				submitReadyExecution(entry);
			}
		}
		for (TargetTeleportExecution entry : result.timedOut()) {
			targetProcessor.finishEntry(entry, TeleportStatus.FAILED);
		}
	}

	private void advancePending() {
		int admissionLimit = currentReadyAdmissionLimit();
		int[] admitted = { 0 };
		operationManager.visitCurrentTargetPendings(pending -> {
			if (admitted[0] >= admissionLimit) {
				return false;
			}
			if (!operationManager.isCurrent(pending.playerUuid(), pending.pendingSequence())) {
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

			TargetTeleportExecution entry = toExecutionEntry(pending, resolved.target());
			if (!pending.isDelayDone(currentTick)) {
				if (shouldStartPreloadDuringDelay(pending) && !pending.isPreloadStarted()
						&& preloadManager.preload(entry, currentTick)) {
					pending.markPreloadStarted();
					admitted[0]++;
				}
				return true;
			}

			if (preloadManager.shouldPreload(resolved.target())) {
				pending.markPreloadStarted();
				if (preloadManager.preload(entry, currentTick)) {
					admitted[0]++;
					return true;
				}
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

	private boolean shouldStartPreloadDuringDelay(TargetTeleportPending pending) {
		return currentTick >= pending.delayUntilTick() - TeleportServiceSettings.PRELOAD_LEAD_TICKS;
	}

	private void updateAdmissionRamp() {
		if (operationManager.hasCurrentOperations() || dispatcher.queueSize() > 0 || preloadManager.activeTicketCount() > 0) {
			admissionRampTick++;
		} else {
			admissionRampTick = 0;
		}
	}

	private void submitReadyExecution(TargetTeleportExecution entry) {
		if (!operationManager.isCurrent(entry.playerUuid(), entry.pendingSequence())) {
			return;
		}

		if (!operationManager.markTargetQueuedIfCurrentAndDelayDone(entry.playerUuid(), entry.pendingSequence(), currentTick)) {
			return;
		}
		if (dispatcher.canUseFastPath()) {
			dispatcher.noteFastPathUse();
			targetProcessor.executeOne(entry.target().world().getServer(), entry, currentTick);
		} else {
			dispatcher.enqueue(entry);
		}
	}

	private TargetTeleportExecution toExecutionEntry(TargetTeleportPending pending, TeleportTarget target) {
		return new TargetTeleportExecution(pending, target);
	}

	private void finishPending(TargetTeleportPending pending, TeleportStatus status) {
		cancelPending(pending.playerUuid(), pending.pendingSequence(), status);
	}
}
