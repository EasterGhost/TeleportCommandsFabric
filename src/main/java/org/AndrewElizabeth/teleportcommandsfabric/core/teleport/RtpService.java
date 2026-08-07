package org.AndrewElizabeth.teleportcommandsfabric.core.teleport;

import org.AndrewElizabeth.teleportcommandsfabric.core.record.AsyncRecordedLocationSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportOperationManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportPreloadManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportExecutor;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.rtp.RtpExecutionProcessor;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.rtp.RtpRequest;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.rtp.RtpTeleportPending;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportOperation;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public final class RtpService {
	public static final int DEFAULT_MAX_ATTEMPTS = 4096;

	private final TeleportOperationManager operationManager;
	private final TeleportPreloadManager preloadManager;
	private final RtpExecutionProcessor executionProcessor;
	private final ArrayDeque<PendingRef> pendingQueue = new ArrayDeque<>();
	private long currentTick;

	public RtpService(AsyncRecordedLocationSource recordedSource, TeleportOperationManager operationManager,
			TeleportPreloadManager preloadManager) {
		this(recordedSource, operationManager, preloadManager, createParallelExecutor());
	}

	RtpService(AsyncRecordedLocationSource recordedSource, TeleportOperationManager operationManager,
			TeleportPreloadManager preloadManager, ExecutorService parallelExecutor) {
		this.operationManager = Objects.requireNonNull(operationManager, "operationManager");
		this.preloadManager = Objects.requireNonNull(preloadManager, "preloadManager");
		TeleportExecutor executor = new TeleportExecutor(recordedSource, this.operationManager);
		this.executionProcessor = new RtpExecutionProcessor(this.operationManager, executor,
				Objects.requireNonNull(parallelExecutor, "parallelExecutor"));
	}

	public CompletableFuture<TeleportStatus> request(ServerPlayer player, RtpRequest request) {
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
			return CompletableFuture.failedFuture(new IllegalStateException("RtpService.request must be called on the server thread"));
		}

		return request(server, snapshot(player, request));
	}

	private CompletableFuture<TeleportStatus> request(MinecraftServer server, RtpOperationSnapshot snapshot) {
		ServerPlayer player = server.getPlayerList().getPlayer(snapshot.playerUuid());
		if (player == null) {
			return CompletableFuture.completedFuture(TeleportStatus.PLAYER_DISCONNECTED);
		}
		if (player.isDeadOrDying()) {
			return CompletableFuture.completedFuture(TeleportStatus.CANCELLED_BY_EVENT);
		}
		if (!player.level().dimension().equals(snapshot.dimension())) {
			return CompletableFuture.completedFuture(TeleportStatus.TARGET_UNAVAILABLE);
		}

		long remainingCooldown = operationManager.getRemainingCooldownMillis(snapshot.playerUuid(), snapshot.cooldownMillis());
		if (remainingCooldown > 0L) {
			return CompletableFuture.completedFuture(TeleportStatus.COOLDOWN);
		}

		TeleportOperationManager.OperationCreateResult<RtpTeleportPending> createResult = operationManager.createOperation(
				snapshot.playerUuid(), currentTick,
				(sequence, tick) -> new RtpTeleportPending(snapshot.playerUuid(), sequence, tick, snapshot.delayTicks(),
						snapshot.cooldownMillis(), snapshot.recordPrevious(), snapshot.center(), snapshot.dimension(),
						snapshot.minRadius(), snapshot.maxRadius(), snapshot.maxAttempts()));
		createResult.replaced().ifPresent(this::releaseReplacedTargetPreload);
		pendingQueue.addLast(new PendingRef(snapshot.playerUuid(), createResult.pending().pendingSequence()));
		return createResult.pending().resultFuture();
	}

	public void tick(MinecraftServer server) {
		if (server == null) {
			return;
		}
		currentTick++;
		promoteReadyPendings();
		executionProcessor.tick(server);
	}

	public void onPlayerQuit(UUID playerUuid) {
		if (playerUuid == null) {
			return;
		}
		pendingQueue.removeIf(ref -> ref.playerUuid().equals(playerUuid));
		executionProcessor.onPlayerQuit(playerUuid);
	}

	public void clear() {
		pendingQueue.clear();
		executionProcessor.clear();
	}

	public void shutdown() {
		pendingQueue.clear();
		executionProcessor.shutdown();
	}

	private void promoteReadyPendings() {
		while (!pendingQueue.isEmpty()) {
			PendingRef ref = pendingQueue.peekFirst();
			Optional<RtpTeleportPending> pending = currentPending(ref);
			if (pending.isEmpty()) {
				pendingQueue.pollFirst();
				continue;
			}
			if (!pending.get().isDelayDone(currentTick)) {
				break;
			}
			pendingQueue.pollFirst();
			executionProcessor.addReady(pending.get());
		}
	}

	private Optional<RtpTeleportPending> currentPending(PendingRef ref) {
		return operationManager.getCurrentOperation(ref.playerUuid(), RtpTeleportPending.class)
				.filter(pending -> pending.pendingSequence() == ref.pendingSequence());
	}

	private void releaseReplacedTargetPreload(TeleportOperation replaced) {
		preloadManager.release(replaced.playerUuid(), replaced.pendingSequence());
	}

	private static ExecutorService createParallelExecutor() {
		ThreadFactory factory = Thread.ofVirtual().name("TCF-RTP-", 0).factory();
		return Executors.newThreadPerTaskExecutor(factory);
	}

	private RtpOperationSnapshot snapshot(ServerPlayer player, RtpRequest request) {
		return new RtpOperationSnapshot(player.getUUID(), player.blockPosition(), player.level().dimension(),
				request.minRadius(), request.maxRadius(), request.maxAttempts(), request.delayTicks(),
				request.cooldownMillis(), request.recordPrevious());
	}

	private record PendingRef(UUID playerUuid, long pendingSequence) {
	}

	private record RtpOperationSnapshot(UUID playerUuid, BlockPos center, ResourceKey<Level> dimension, int minRadius,
			int maxRadius, int maxAttempts, int delayTicks, long cooldownMillis, boolean recordPrevious) {
		private RtpOperationSnapshot {
			Objects.requireNonNull(playerUuid, "playerUuid");
			center = Objects.requireNonNull(center, "center").immutable();
			Objects.requireNonNull(dimension, "dimension");
		}
	}
}
