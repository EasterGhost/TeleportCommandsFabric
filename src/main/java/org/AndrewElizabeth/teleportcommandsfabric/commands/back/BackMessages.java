package org.AndrewElizabeth.teleportcommandsfabric.commands.back;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

public final class BackMessages {

	private BackMessages() {
	}

	public static boolean ensureEnabled(ServerPlayer player, boolean enabled) {
		if (enabled) {
			return true;
		}
		send(player, "commands.teleport_commands.back.disabled", ChatFormatting.RED);
		return false;
	}

	public static int execute(ServerPlayer player, String errorLogMessage, BackAction action) {
		try {
			return action.run();
		} catch (Exception e) {
			Constants.LOGGER.error(errorLogMessage, e);
			send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
	}

	public static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		player.displayClientMessage(getTranslatedText(key, player).withStyle(formatting), true);
	}

	public static void sendNoLocation(ServerPlayer player) {
		send(player, "commands.teleport_commands.common.noLocation", ChatFormatting.RED);
	}

	public static void sendWorldNotFound(ServerPlayer player) {
		send(player, "commands.teleport_commands.common.worldNotFound", ChatFormatting.RED, ChatFormatting.BOLD);
	}

	public static void sendSame(ServerPlayer player) {
		send(player, "commands.teleport_commands.back.same", ChatFormatting.AQUA);
	}

	public static void sendGo(ServerPlayer player) {
		player.displayClientMessage(getTranslatedText("commands.teleport_commands.back.go", player), true);
	}

	public static void sendUnsafeTeleportPrompt(ServerPlayer player) {
		player.displayClientMessage(
				Component.empty()
						.append(getTranslatedText("commands.teleport_commands.common.noSafeLocation", player)
								.withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
						.append("\n")
						.append(getTranslatedText("commands.teleport_commands.common.safetyIsForLosers", player)
								.withStyle(ChatFormatting.WHITE))
						.append("\n")
						.append(getTranslatedText("commands.teleport_commands.common.forceTeleport", player)
								.withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)
								.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand("back true"))))
						.append("\n"),
				false);
	}

	@FunctionalInterface
	public interface BackAction {
		int run() throws Exception;
	}
}
