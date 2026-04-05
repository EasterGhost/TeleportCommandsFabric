package org.AndrewElizabeth.teleportcommandsfabric.modules.back;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

public final class BackMessages {
	private static final String COMMAND_BACK_TP = "back tp";
	private static final String DISPLAY_COMMAND_BACK_TP = "/back tp";

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
			ModConstants.LOGGER.error(errorLogMessage, e);
			send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
	}

	public static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		player.sendSystemMessage(getTranslatedText(key, player).withStyle(formatting), true);
	}

	public static void sendNoLocation(ServerPlayer player) {
		send(player, "commands.teleport_commands.common.noLocation", ChatFormatting.RED);
	}

	public static void sendTryBackTpPrompt(ServerPlayer player) {
		player.sendSystemMessage(Component.empty()
				.append(getTranslatedText("commands.teleport_commands.back.tryTp", player)
						.withStyle(ChatFormatting.YELLOW))
				.append(" ")
				.append(Component.literal("[" + DISPLAY_COMMAND_BACK_TP + "] ?")
						.withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)
						.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(COMMAND_BACK_TP))
								.withHoverEvent(new HoverEvent.ShowText(
										Component.literal(DISPLAY_COMMAND_BACK_TP))))),
				false);
	}

	public static void sendNoPreviousTeleportLocation(ServerPlayer player) {
		send(player, "commands.teleport_commands.back.tp.none", ChatFormatting.RED);
	}

	public static void sendWorldNotFound(ServerPlayer player) {
		send(player, "commands.teleport_commands.common.worldNotFound", ChatFormatting.RED, ChatFormatting.BOLD);
	}

	public static void sendSame(ServerPlayer player) {
		send(player, "commands.teleport_commands.back.same", ChatFormatting.AQUA);
	}

	public static void sendPreviousTeleportSame(ServerPlayer player) {
		send(player, "commands.teleport_commands.back.tp.same", ChatFormatting.AQUA);
	}

	public static void sendGo(ServerPlayer player) {
		player.sendSystemMessage(getTranslatedText("commands.teleport_commands.back.go", player), true);
	}

	public static void sendPreviousTeleportGo(ServerPlayer player) {
		player.sendSystemMessage(getTranslatedText("commands.teleport_commands.back.tp.go", player), true);
	}

	public static void sendUnsafeTeleportPrompt(ServerPlayer player, String command) {
		player.sendSystemMessage(
				Component.empty()
						.append(getTranslatedText("commands.teleport_commands.common.noSafeLocation", player)
								.withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
						.append("\n")
						.append(getTranslatedText("commands.teleport_commands.common.safetyIsForLosers", player)
								.withStyle(ChatFormatting.WHITE))
						.append("\n")
						.append(getTranslatedText("commands.teleport_commands.common.forceTeleport", player)
								.withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)
								.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(command))))
						.append("\n"),
				false);
	}

	@FunctionalInterface
	public interface BackAction {
		int run() throws Exception;
	}
}
