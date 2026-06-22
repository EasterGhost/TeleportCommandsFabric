package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.core.record.AsyncRecordedLocationSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportOperationManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportOperation;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

public final class TeleportExecutor {
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

		if (operation.recordPrevious() && recordedSource != null) {
			recordedSource.recordPreviousTeleportLocation(player.getUUID(), player.blockPosition(), player.level().dimension(),
					player.getYRot(), player.getXRot())
					.whenComplete((ignored, throwable) -> {
						if (throwable != null) {
							ModConstants.LOGGER.warn("Failed to record previous teleport location", throwable);
						}
					});
		}

		boolean flying = player.getAbilities().flying;
		boolean effectsEnabled = TeleportEffects.isEnabled();
		if (effectsEnabled) {
			TeleportEffects.playBefore(player);
		}
		boolean restoreRotation = TeleportRotation.isRestoreEnabled();
		float yRot = restoreRotation ? target.effectiveYRot(player) : player.getYRot();
		float xRot = restoreRotation ? target.effectiveXRot(player) : player.getXRot();
		boolean teleported = player.teleportTo(target.world(), target.position().x(), target.position().y(), target.position().z(),
				Set.of(), yRot, xRot, false);
		if (!teleported) {
			return finishOperation(operation, TeleportStatus.FAILED);
		}
		if (effectsEnabled) {
			TeleportEffects.playAfter(player);
		}
		if (flying) {
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
		}

		operationManager.markSuccess(operation.playerUuid(), operation.pendingSequence());
		operation.resultFuture().complete(TeleportStatus.SUCCESS);
		return TeleportStatus.SUCCESS;
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
}
