package org.AndrewElizabeth.teleportcommandsfabric.commands.rtp;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

public final class RtpMessages {

	private RtpMessages() {
	}

	public static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		player.sendSystemMessage(getTranslatedText(key, player).withStyle(formatting), true);
	}

	public static int execute(ServerPlayer player, String logMessage, RtpAction action) {
		try {
			return action.run();
		} catch (Exception e) {
			Constants.LOGGER.error(logMessage, e);
			send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
	}

	@FunctionalInterface
	public interface RtpAction {
		int run() throws Exception;
	}
}
