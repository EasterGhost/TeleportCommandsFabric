package org.AndrewElizabeth.teleportcommandsfabric.commands.tpa;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

final class TpaMessages {
	@FunctionalInterface
	interface TpaAction {
		void run() throws Exception;
	}

	private TpaMessages() {
	}

	static boolean ensureEnabled(ServerPlayer player) {
		if (ConfigManager.CONFIG.getTpa().isEnabled()) {
			return true;
		}
		player.displayClientMessage(
				getTranslatedText("commands.teleport_commands.tpa.disabled", player)
						.withStyle(ChatFormatting.RED),
				true);
		return false;
	}

	static int execute(String errorLogMessage, TpaAction action) {
		try {
			action.run();
			return 0;
		} catch (Exception e) {
			Constants.LOGGER.error(errorLogMessage, e);
			return 1;
		}
	}
}
