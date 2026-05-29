package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportOperationManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.rtp.RtpTeleportPending;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class RtpExecutionProcessor {
	private static final int MAX_ACTIVE_RTP = 16;
	private static final int PARALLEL_BACKLOG_THRESHOLD = MAX_ACTIVE_RTP * 2;
	private static final int ATTEMPTS_PER_TICK = 256;

	private final TeleportOperationManager operationManager;
	private final TeleportExecutor executor;
	private final ExecutorService parallelExecutor;
	private final ArrayDeque<PendingRef> readyBacklog = new ArrayDeque<>();
	private final ArrayDeque<PendingRef> activeExecutions = new ArrayDeque<>();

	public RtpExecutionProcessor(TeleportOperationManager operationManager, TeleportExecutor executor,
			ExecutorService parallelExecutor) {
		this.operationManager = Objects.requireNonNull(operationManager, "operationManager");
		this.executor = Objects.requireNonNull(executor, "executor");
		this.parallelExecutor = Objects.requireNonNull(parallelExecutor, "parallelExecutor");
	}

	public void addReady(RtpTeleportPending pending) {
		readyBacklog.addLast(new PendingRef(pending.playerUuid(), pending.pendingSequence()));
	}

	public void tick(MinecraftServer server) {
		if (readyBacklog.size() >= PARALLEL_BACKLOG_THRESHOLD) {
			runParallelBacklog(server);
		} else {
			fillSerialActiveSlots();
		}
		runSerialActive(server);
	}

	public void onPlayerQuit(UUID playerUuid) {
		for (RtpTeleportPending pending : operationManager.currentOperations(RtpTeleportPending.class)) {
			if (pending.playerUuid().equals(playerUuid)) {
				executor.finishOperation(pending, TeleportStatus.CANCELLED);
			}
		}
		readyBacklog.removeIf(ref -> ref.playerUuid().equals(playerUuid));
		activeExecutions.removeIf(ref -> ref.playerUuid().equals(playerUuid));
	}

	public void clear() {
		readyBacklog.clear();
		activeExecutions.clear();
		for (RtpTeleportPending pending : operationManager.currentOperations(RtpTeleportPending.class)) {
			executor.finishOperation(pending, TeleportStatus.CANCELLED);
		}
	}

	public void shutdown() {
		clear();
		parallelExecutor.shutdownNow();
	}

	public int readyBacklogSize() {
		return readyBacklog.size();
	}

	public int activeExecutionCount() {
		return activeExecutions.size();
	}

	private void runSerialActive(MinecraftServer server) {
		int activeCount = activeExecutions.size();
		for (int i = 0; i < activeCount; i++) {
			PendingRef ref = activeExecutions.pollFirst();
			Optional<RtpTeleportPending> pending = currentPending(ref);
			if (pending.isEmpty()) {
				continue;
			}

			RtpTeleportPending operation = pending.get();
			TeleportStatus invalidStatus = validatePlayer(server, operation);
			if (invalidStatus != null) {
				executor.finishOperation(operation, invalidStatus);
				continue;
			}
			ServerLevel world = server.getLevel(operation.dimension());
			if (world == null) {
				executor.finishOperation(operation, TeleportStatus.TARGET_UNAVAILABLE);
				continue;
			}
			int budget = operation.consumeAttempts(ATTEMPTS_PER_TICK);
			Optional<BlockPos> safePos;
			try {
				safePos = RtpPositionFinder.findSafeRandomPosition(world, operation, budget, operation.random());
			} catch (RuntimeException exception) {
				executor.finishOperation(operation, TeleportStatus.FAILED);
				continue;
			}
			if (safePos.isPresent()) {
				executeAt(server, operation, safePos.get());
				continue;
			}
			if (operation.isExhausted()) {
				executor.finishOperation(operation, TeleportStatus.NO_SAFE_POSITION);
				continue;
			}
			activeExecutions.addLast(ref);
		}
	}

	private void fillSerialActiveSlots() {
		while (activeExecutions.size() < MAX_ACTIVE_RTP && !readyBacklog.isEmpty()) {
			PendingRef ref = readyBacklog.pollFirst();
			if (currentPending(ref).isPresent()) {
				activeExecutions.addLast(ref);
			}
		}
	}

	private void runParallelBacklog(MinecraftServer server) {
		List<RtpTeleportPending> batch = new ArrayList<>(readyBacklog.size());
		while (!readyBacklog.isEmpty()) {
			currentPending(readyBacklog.pollFirst()).ifPresent(batch::add);
		}
		if (batch.isEmpty()) {
			return;
		}

		List<CompletableFuture<ParallelResult>> futures = new ArrayList<>(batch.size());
		for (RtpTeleportPending pending : batch) {
			TeleportStatus invalidStatus = validatePlayer(server, pending);
			if (invalidStatus != null) {
				executor.finishOperation(pending, invalidStatus);
				continue;
			}
			ServerLevel world = server.getLevel(pending.dimension());
			if (world == null) {
				executor.finishOperation(pending, TeleportStatus.TARGET_UNAVAILABLE);
				continue;
			}
			int budget = pending.consumeAttempts(pending.remainingAttempts());
			futures.add(CompletableFuture.supplyAsync(() -> {
				try {
					return new ParallelResult(pending, RtpPositionFinder.findSafeRandomPosition(world, pending, budget, pending.random()), null);
				} catch (RuntimeException exception) {
					return new ParallelResult(pending, Optional.empty(), TeleportStatus.FAILED);
				}
			}, parallelExecutor));
		}
		CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

		for (CompletableFuture<ParallelResult> future : futures) {
			ParallelResult result = future.join();
			if (!operationManager.isCurrent(result.pending())) {
				continue;
			}
			if (result.failureStatus() != null) {
				executor.finishOperation(result.pending(), result.failureStatus());
				continue;
			}
			if (result.safePos().isPresent()) {
				executeAt(server, result.pending(), result.safePos().get());
			} else {
				executor.finishOperation(result.pending(), TeleportStatus.NO_SAFE_POSITION);
			}
		}
	}

	private Optional<RtpTeleportPending> currentPending(PendingRef ref) {
		return operationManager.getCurrentOperation(ref.playerUuid(), RtpTeleportPending.class)
				.filter(pending -> pending.pendingSequence() == ref.pendingSequence());
	}

	private TeleportStatus validatePlayer(MinecraftServer server, RtpTeleportPending pending) {
		ServerPlayer player = server.getPlayerList().getPlayer(pending.playerUuid());
		if (player == null) {
			return TeleportStatus.PLAYER_DISCONNECTED;
		}
		if (player.isDeadOrDying()) {
			return TeleportStatus.CANCELLED_BY_EVENT;
		}
		if (!player.level().dimension().equals(pending.dimension())) {
			return TeleportStatus.TARGET_UNAVAILABLE;
		}
		return null;
	}

	private void executeAt(MinecraftServer server, RtpTeleportPending pending, BlockPos safePos) {
		ServerLevel world = server.getLevel(pending.dimension());
		if (world == null) {
			executor.finishOperation(pending, TeleportStatus.TARGET_UNAVAILABLE);
			return;
		}
		executor.executeResolved(server, pending, TeleportTarget.centered(world, safePos));
	}

	private record PendingRef(UUID playerUuid, long pendingSequence) {
	}

	private record ParallelResult(RtpTeleportPending pending, Optional<BlockPos> safePos, TeleportStatus failureStatus) {
	}
}
