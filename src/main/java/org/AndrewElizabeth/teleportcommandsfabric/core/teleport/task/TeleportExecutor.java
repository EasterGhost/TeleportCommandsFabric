package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.core.record.AsyncRecordedLocationSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportOperationManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportOperation;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Set;

public final class TeleportExecutor {
	private static final boolean RESET_CAMERA = true;

	private final AsyncRecordedLocationSource recordedSource;
	private final TeleportOperationManager operationManager;

	public TeleportExecutor(AsyncRecordedLocationSource recordedSource, TeleportOperationManager operationManager) {
		this.recordedSource = recordedSource;
		this.operationManager = operationManager;
	}

	public TeleportStatus executeResolved(MinecraftServer server, TeleportOperation operation, TeleportTarget target) {
		if (!operationManager.isCurrent(operation)) {
			return TeleportStatus.CANCELLED;
		}

		ServerPlayer player = server.getPlayerList().getPlayer(operation.playerUuid());
		if (player == null) {
			return finishOperation(operation, TeleportStatus.PLAYER_DISCONNECTED);
		}
		if (player.isDeadOrDying()) {
			return finishOperation(operation, TeleportStatus.CANCELLED_BY_EVENT);
		}

		if (server.getLevel(target.world().dimension()) == null) {
			return finishOperation(operation, TeleportStatus.TARGET_UNAVAILABLE);
		}

		boolean flying = player.getAbilities().flying;
		stopFallFlying(player);
		boolean effectsEnabled = TeleportEffects.isEnabled();
		if (effectsEnabled) {
			TeleportEffects.playBefore(player);
		}
		PreviousLocation previousLocation = operation.recordPrevious() && recordedSource != null
				? PreviousLocation.capture(player)
				: null;
		boolean restoreRotation = TeleportRotation.isRestoreEnabled();
		float yRot = restoreRotation ? target.effectiveYRot(player) : player.getYRot();
		float xRot = restoreRotation ? target.effectiveXRot(player) : player.getXRot();
		boolean teleported = player.teleportTo(target.world(), target.position().x(), target.position().y(), target.position().z(),
				Set.of(), yRot, xRot, RESET_CAMERA);
		if (!teleported) {
			return finishOperation(operation, TeleportStatus.FAILED);
		}
		recordPrevious(operation, previousLocation);
		if (effectsEnabled) {
			TeleportEffects.playAfter(player);
		}
		stopFallFlying(player);
		if (flying) {
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
		}

		operationManager.markSuccess(operation.playerUuid(), operation.pendingSequence());
		operation.resultFuture().complete(TeleportStatus.SUCCESS);
		return TeleportStatus.SUCCESS;
	}

	private static void stopFallFlying(ServerPlayer player) {
		if (player.isFallFlying()) {
			player.stopFallFlying();
		}
	}

	private void recordPrevious(TeleportOperation operation, PreviousLocation previousLocation) {
		if (previousLocation == null) {
			return;
		}
		recordedSource.recordPreviousTeleportLocation(operation.playerUuid(), previousLocation.pos(), previousLocation.dimension(),
				previousLocation.yRot(), previousLocation.xRot())
				.whenComplete((ignored, throwable) -> {
					if (throwable != null) {
						ModConstants.LOGGER.warn("Failed to record previous teleport location", throwable);
					}
				});
	}

	public TeleportStatus finishOperation(TeleportOperation operation, TeleportStatus status) {
		if (status == TeleportStatus.SUCCESS) {
			operationManager.markSuccess(operation.playerUuid(), operation.pendingSequence());
			operation.resultFuture().complete(TeleportStatus.SUCCESS);
		} else {
			operationManager.cancelPending(operation.playerUuid(), operation.pendingSequence(), status);
		}
		return status;
	}

	private record PreviousLocation(BlockPos pos, ResourceKey<Level> dimension, float yRot, float xRot) {
		private static PreviousLocation capture(ServerPlayer player) {
			return new PreviousLocation(player.blockPosition(), player.level().dimension(), player.getYRot(), player.getXRot());
		}
	}
}
