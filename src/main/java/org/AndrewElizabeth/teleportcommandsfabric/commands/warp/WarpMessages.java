package org.AndrewElizabeth.teleportcommandsfabric.commands.warp;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

final class WarpMessages {
	@FunctionalInterface
	interface WarpAction {
		void run() throws Exception;
	}

	private WarpMessages() {
	}

	static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		player.displayClientMessage(getTranslatedText(key, player).withStyle(formatting), true);
	}

	static boolean ensureEnabled(ServerPlayer player) {
		if (ConfigManager.CONFIG.getWarp().isEnabled()) {
			return true;
		}
		send(player, "commands.teleport_commands.warp.disabled", ChatFormatting.RED);
		return false;
	}

	static void sendHomeless(ServerPlayer player) {
		send(player, "commands.teleport_commands.warp.homeless", ChatFormatting.AQUA);
	}

	static void sendNotFound(ServerPlayer player) {
		send(player, "commands.teleport_commands.warp.notFound", ChatFormatting.RED);
	}

	static void sendNameExists(ServerPlayer player) {
		send(player, "commands.teleport_commands.common.nameExists", ChatFormatting.RED);
	}

	static void sendMaxReached(ServerPlayer player, int maxWarps) {
		player.displayClientMessage(
				getTranslatedText("commands.teleport_commands.warp.max", player,
						Component.literal(String.valueOf(maxWarps)))
						.withStyle(ChatFormatting.RED),
				true);
	}

	static void sendWorldNotFound(ServerPlayer player) {
		send(player, "commands.teleport_commands.common.worldNotFound", ChatFormatting.RED, ChatFormatting.BOLD);
	}

	static void sendDeletedInvalid(ServerPlayer player) {
		send(player, "commands.teleport_commands.warp.deletedInvalid", ChatFormatting.YELLOW);
	}

	static void sendMapVisibilityAlready(ServerPlayer player, boolean visible) {
		send(player,
				visible
						? "commands.teleport_commands.warp.mapAlreadyShown"
						: "commands.teleport_commands.warp.mapAlreadyHidden",
				ChatFormatting.AQUA);
	}

	static void sendMapVisibilityChanged(ServerPlayer player, boolean visible) {
		send(player,
				visible
						? "commands.teleport_commands.warp.mapShown"
						: "commands.teleport_commands.warp.mapHidden",
				ChatFormatting.GREEN);
	}

	static int execute(ServerPlayer player, String errorLogMessage, String errorTranslationKey, WarpAction action) {
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
}
