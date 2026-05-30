package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class HomeMessages {
	private HomeMessages() {
	}

	public static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		if (player != null) {
			player.sendSystemMessage(TranslationHelper.getTranslatedText(key, player).withStyle(formatting), true);
		}
	}

	public static void sendMaxReached(ServerPlayer player, int maxHomes) {
		if (player != null) {
			player.sendSystemMessage(TranslationHelper.getTranslatedText("commands.teleport_commands.home.max", player,
					Component.literal(String.valueOf(maxHomes))).withStyle(ChatFormatting.RED), true);
		}
	}

	public static void sendNoHomesInDimension(ServerPlayer player, String dimensionFilter) {
		if (player != null) {
			player.sendSystemMessage(TranslationHelper.getTranslatedText("commands.teleport_commands.home.noneInDimension", player,
					Component.literal(dimensionFilter).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.AQUA), true);
		}
	}

	public static void sendDelayStart(ServerPlayer player, int delaySeconds) {
		if (player != null) {
			player.sendSystemMessage(TranslationHelper.getTranslatedText("commands.teleport_commands.common.delayStart",
					player, Component.literal(String.valueOf(delaySeconds))).withStyle(ChatFormatting.AQUA), true);
		}
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

	public static void sendStatus(ServerPlayer player, TeleportStatus status, int cooldownSeconds, String forceCommand) {
		if (player == null || status == null || status == TeleportStatus.SUCCESS || status == TeleportStatus.CANCELLED) {
			return;
		}
		switch (status) {
		case COOLDOWN -> player.sendSystemMessage(TranslationHelper.getTranslatedText(
				"commands.teleport_commands.common.cooldown", player,
				Component.literal(String.valueOf(cooldownSeconds))).withStyle(ChatFormatting.YELLOW), true);
		case NO_SAFE_POSITION -> sendUnsafeTeleportPrompt(player, forceCommand);
		case TARGET_UNAVAILABLE -> send(player, "commands.teleport_commands.home.notFound", ChatFormatting.AQUA);
		case PLAYER_DISCONNECTED -> {
		}
		default -> send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}
}
