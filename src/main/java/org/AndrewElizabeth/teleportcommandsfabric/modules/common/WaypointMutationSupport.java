package org.AndrewElizabeth.teleportcommandsfabric.modules.common;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointOperationResult;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class WaypointMutationSupport {
	private WaypointMutationSupport() {
	}

	public static void handle(ServerPlayer requester, CompletionStage<WaypointOperationResult> future,
			String failureLog, Runnable successEffect,
			CommandAsyncSupport.PlayerCompletion<WaypointOperationResult> playerCompletion) {
		if (requester == null || future == null || playerCompletion == null) {
			return;
		}
		MinecraftServer server = requester.level().getServer();
		UUID playerUuid = requester.getUUID();
		if (server == null) {
			return;
		}

		future.whenComplete((result, throwable) -> server.execute(() -> {
			if (throwable != null) {
				ModConstants.LOGGER.error(failureLog, throwable);
			} else if (result == WaypointOperationResult.SUCCESS && successEffect != null) {
				try {
					successEffect.run();
				} catch (RuntimeException exception) {
					ModConstants.LOGGER.error("Failed to apply waypoint mutation side effects.", exception);
				}
			}

			ServerPlayer currentPlayer = server.getPlayerList().getPlayer(playerUuid);
			if (currentPlayer == null) {
				return;
			}
			try {
				playerCompletion.accept(currentPlayer, result, throwable);
			} catch (RuntimeException exception) {
				ModConstants.LOGGER.error("Failed to handle waypoint mutation result for {}.", playerUuid, exception);
			}
		}));
	}
}
