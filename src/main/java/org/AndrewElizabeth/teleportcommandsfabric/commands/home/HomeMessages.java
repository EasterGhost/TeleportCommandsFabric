package org.AndrewElizabeth.teleportcommandsfabric.commands.home;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

final class HomeMessages {
	@FunctionalInterface
	interface HomeAction {
		void run() throws Exception;
	}

	private HomeMessages() {
	}

	static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		player.displayClientMessage(getTranslatedText(key, player).withStyle(formatting), true);
	}

	static boolean ensureEnabled(ServerPlayer player) {
		if (ConfigManager.CONFIG.getHome().isEnabled()) {
			return true;
		}
		send(player, "commands.teleport_commands.home.disabled", ChatFormatting.RED);
		return false;
	}

	static void sendHomeless(ServerPlayer player) {
		send(player, "commands.teleport_commands.home.homeless", ChatFormatting.AQUA);
	}

	static void sendNameExists(ServerPlayer player) {
		send(player, "commands.teleport_commands.common.nameExists", ChatFormatting.RED);
	}

	static void sendMaxReached(ServerPlayer player, int maxHomes) {
		player.displayClientMessage(
				getTranslatedText("commands.teleport_commands.home.max", player,
						Component.literal(String.valueOf(maxHomes)))
						.withStyle(ChatFormatting.RED),
				true);
	}

	static void sendWorldNotFound(ServerPlayer player) {
		send(player, "commands.teleport_commands.common.worldNotFound", ChatFormatting.RED, ChatFormatting.BOLD);
	}

	static void sendDeletedInvalid(ServerPlayer player) {
		send(player, "commands.teleport_commands.home.deletedInvalid", ChatFormatting.YELLOW);
	}

	static void sendNotFound(ServerPlayer player, ChatFormatting color) {
		send(player, "commands.teleport_commands.home.notFound", color);
	}

	static void sendMapVisibilityAlready(ServerPlayer player, boolean visible) {
		send(player,
				visible
						? "commands.teleport_commands.home.mapAlreadyShown"
						: "commands.teleport_commands.home.mapAlreadyHidden",
				ChatFormatting.AQUA);
	}

	static void sendMapVisibilityChanged(ServerPlayer player, boolean visible) {
		send(player,
				visible
						? "commands.teleport_commands.home.mapShown"
						: "commands.teleport_commands.home.mapHidden",
				ChatFormatting.GREEN);
	}

	static int execute(ServerPlayer player, String errorLogMessage, String errorTranslationKey, HomeAction action) {
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

	static int executeSilently(String errorLogMessage, HomeAction action) {
		try {
			action.run();
			return 0;
		} catch (Exception e) {
			Constants.LOGGER.error(errorLogMessage, e);
			return 1;
		}
	}
}
