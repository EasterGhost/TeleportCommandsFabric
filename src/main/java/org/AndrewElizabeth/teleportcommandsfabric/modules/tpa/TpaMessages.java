package org.AndrewElizabeth.teleportcommandsfabric.modules.tpa;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;
import static org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG;

final class TpaMessages {
	@FunctionalInterface
	interface TpaAction {
		void run() throws Exception;
	}

	private TpaMessages() {
	}

	static boolean ensureEnabled(ServerPlayer player) {
		if (CONFIG.getTpa().isEnabled()) {
			return true;
		}
		player.sendSystemMessage(getTranslatedText("commands.teleport_commands.tpa.disabled", player)
				.withStyle(ChatFormatting.RED), true);
		return false;
	}

	static int execute(String errorLogMessage, TpaAction action) {
		try {
			action.run();
			return 0;
		} catch (Exception e) {
			ModConstants.LOGGER.error(errorLogMessage, e);
			return 1;
		}
	}
}

