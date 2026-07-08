package org.AndrewElizabeth.teleportcommandsfabric.modules.common;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class CommandAsyncSupport {
	private CommandAsyncSupport() {
	}

	public static <T> void whenCompleteForPlayer(ServerPlayer player, CompletionStage<T> future,
			PlayerCompletion<T> completion) {
		if (player == null || future == null || completion == null) {
			return;
		}
		whenCompleteForPlayer(player.level().getServer(), player.getUUID(), future, completion);
	}

	public static <T> void whenCompleteForPlayer(MinecraftServer server, UUID playerUuid,
			CompletionStage<T> future, PlayerCompletion<T> completion) {
		if (server == null || playerUuid == null || future == null || completion == null) {
			return;
		}
		future.whenComplete((result, throwable) -> server.execute(() -> {
			try {
				ServerPlayer currentPlayer = server.getPlayerList().getPlayer(playerUuid);
				if (currentPlayer == null) {
					return;
				}
				completion.accept(currentPlayer, result, throwable);
			} catch (RuntimeException exception) {
				ModConstants.LOGGER.error("Failed to handle async command completion for {}.", playerUuid, exception);
			}
		}));
	}

	@FunctionalInterface
	public interface PlayerCompletion<T> {
		void accept(ServerPlayer player, T result, Throwable throwable);
	}
}
