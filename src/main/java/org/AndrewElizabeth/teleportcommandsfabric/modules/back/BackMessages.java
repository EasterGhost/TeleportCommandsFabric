package org.AndrewElizabeth.teleportcommandsfabric.modules.back;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

public final class BackMessages {
	private static final String COMMAND_BACK_TP = "back tp";
	private static final String DISPLAY_COMMAND_BACK_TP = "/back tp";

	private BackMessages() {
	}

	public static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		if (player != null) {
			player.sendSystemMessage(TranslationHelper.getTranslatedText(key, player).withStyle(formatting), true);
		}
	}

	public static void sendDelayStart(ServerPlayer player, int delaySeconds) {
		if (player != null) {
			player.sendSystemMessage(TranslationHelper.getTranslatedText("commands.teleport_commands.common.delayStart",
					player, Component.literal(String.valueOf(delaySeconds))).withStyle(ChatFormatting.AQUA), true);
		}
	}

	public static void sendTryBackTpPrompt(ServerPlayer player) {
		if (player == null) {
			return;
		}
		player.sendSystemMessage(Component.empty()
				.append(TranslationHelper.getTranslatedText("commands.teleport_commands.back.tryTp", player)
						.withStyle(ChatFormatting.YELLOW))
				.append(" ")
				.append(Component.literal("[" + DISPLAY_COMMAND_BACK_TP + "] ?")
						.withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)
						.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(COMMAND_BACK_TP))
								.withHoverEvent(new HoverEvent.ShowText(Component.literal(DISPLAY_COMMAND_BACK_TP))))),
				false);
	}

	public static void sendUnsafeTeleportPrompt(ServerPlayer player, String command) {
		if (player == null) {
			return;
		}
		player.sendSystemMessage(Component.empty()
				.append(TranslationHelper.getTranslatedText("commands.teleport_commands.common.noSafeLocation", player)
						.withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
				.append("\n")
				.append(TranslationHelper.getTranslatedText("commands.teleport_commands.common.safetyIsForLosers", player)
						.withStyle(ChatFormatting.WHITE))
				.append("\n")
				.append(TranslationHelper.getTranslatedText("commands.teleport_commands.common.forceTeleport", player)
						.withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)
						.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(command))))
				.append("\n"), false);
	}

	public static void sendStatus(ServerPlayer player, TeleportStatus status, int cooldownSeconds,
			String forceCommand) {
		if (player == null || status == null || status == TeleportStatus.SUCCESS || status == TeleportStatus.CANCELLED) {
			return;
		}
		switch (status) {
		case COOLDOWN -> player.sendSystemMessage(TranslationHelper.getTranslatedText(
				"commands.teleport_commands.common.cooldown", player,
				Component.literal(String.valueOf(cooldownSeconds))).withStyle(ChatFormatting.YELLOW), true);
		case NO_SAFE_POSITION -> sendUnsafeTeleportPrompt(player, forceCommand);
		case TARGET_UNAVAILABLE -> send(player, "commands.teleport_commands.common.noLocation", ChatFormatting.RED);
		case PLAYER_DISCONNECTED -> {
		}
		default -> send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}
}
