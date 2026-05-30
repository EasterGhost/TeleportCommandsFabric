package org.AndrewElizabeth.teleportcommandsfabric.modules.rtp;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class RtpMessages {
	private RtpMessages() {
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

	public static void sendStatus(ServerPlayer player, TeleportStatus status, int cooldownSeconds) {
		if (player == null || status == null || status == TeleportStatus.SUCCESS || status == TeleportStatus.CANCELLED) {
			return;
		}
		switch (status) {
		case COOLDOWN -> player.sendSystemMessage(TranslationHelper.getTranslatedText(
				"commands.teleport_commands.common.cooldown", player,
				Component.literal(String.valueOf(cooldownSeconds))).withStyle(ChatFormatting.YELLOW), true);
		case NO_SAFE_POSITION -> send(player, "commands.teleport_commands.common.noSafeLocation", ChatFormatting.RED);
		case PLAYER_DISCONNECTED -> {
		}
		default -> send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}
}
