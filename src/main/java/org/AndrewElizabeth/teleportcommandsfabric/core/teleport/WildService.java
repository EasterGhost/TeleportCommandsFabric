package org.AndrewElizabeth.teleportcommandsfabric.core.teleport;

import org.AndrewElizabeth.teleportcommandsfabric.core.record.AsyncRecordedLocationSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportOperationManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportPreloadManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportExecutor;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild.WildExecutionProcessor;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportOperation;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.wild.WildRequest;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.wild.WildTeleportPending;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class WildService {
	private final TeleportOperationManager operationManager;
	private final TeleportPreloadManager preloadManager;
	private final WildExecutionProcessor executionProcessor;
	private long currentTick;

	public WildService(AsyncRecordedLocationSource recordedSource, TeleportOperationManager operationManager,
			TeleportPreloadManager preloadManager) {
		this(operationManager, preloadManager, new WildExecutionProcessor(operationManager,
				new TeleportExecutor(recordedSource, operationManager)));
	}

	WildService(TeleportOperationManager operationManager, TeleportPreloadManager preloadManager,
			WildExecutionProcessor executionProcessor) {
		this.operationManager = operationManager;
		this.preloadManager = preloadManager;
		this.executionProcessor = executionProcessor;
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
			return CompletableFuture.failedFuture(
					new IllegalStateException("WildService.request must be called on the server thread"));
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
		executionProcessor.start(server, pending, currentTick);
		return pending.resultFuture();
	}

	public void tick(MinecraftServer server) {
		if (server == null) {
			return;
		}
		currentTick++;
		executionProcessor.tick(server, currentTick);
	}

	public boolean hasCurrentRequest(UUID playerUuid) {
		return operationManager.getCurrentOperation(playerUuid, WildTeleportPending.class).isPresent();
	}

	public void onPlayerQuit(UUID playerUuid) {
		executionProcessor.onPlayerQuit(playerUuid);
	}

	public void onPlayerChangeLevel(UUID playerUuid, ResourceKey<Level> destination) {
		executionProcessor.onPlayerChangeLevel(playerUuid, destination);
	}

	public void shutdown() {
		executionProcessor.shutdown();
	}

	private void releaseReplacedTargetPreload(TeleportOperation replaced) {
		preloadManager.release(replaced.playerUuid(), replaced.pendingSequence());
	}
}
