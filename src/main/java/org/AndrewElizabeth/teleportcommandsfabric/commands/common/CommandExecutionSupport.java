package org.AndrewElizabeth.teleportcommandsfabric.commands.common;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

public final class CommandExecutionSupport {
	@FunctionalInterface
	public interface CommandAction {
		void run() throws Exception;
	}

	private CommandExecutionSupport() {
	}

	public static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		player.displayClientMessage(getTranslatedText(key, player).withStyle(formatting), true);
	}

	public static void sendInvalidPage(ServerPlayer player, int requestedPage, int totalPages) {
		player.displayClientMessage(
				getTranslatedText("commands.teleport_commands.common.invalidPage", player,
						Component.literal(String.valueOf(requestedPage)),
						Component.literal(String.valueOf(totalPages)))
						.withStyle(ChatFormatting.RED),
				true);
	}

	public static int execute(ServerPlayer player, String errorLogMessage, String errorTranslationKey,
			CommandAction action) {
		try {
			action.run();
			return 0;
		} catch (Exception e) {
			Constants.LOGGER.error(errorLogMessage, e);
			player.displayClientMessage(
					getTranslatedText(errorTranslationKey, player)
							.withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
					true);
			return 1;
		}
	}

	public static int executeSilently(String errorLogMessage, CommandAction action) {
		try {
			action.run();
			return 0;
		} catch (Exception e) {
			Constants.LOGGER.error(errorLogMessage, e);
			return 1;
		}
	}
}
