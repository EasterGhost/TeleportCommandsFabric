package org.AndrewElizabeth.teleportcommandsfabric.modules.worldspawn;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class WorldSpawnMessages {
	private static final String FORCE_COMMAND = "worldspawn true";

	private WorldSpawnMessages() {
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

	public static void sendUnsafeTeleportPrompt(ServerPlayer player) {
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
						.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(FORCE_COMMAND))))
				.append("\n"), false);
	}

	public static void sendStatus(ServerPlayer player, TeleportStatus status, int cooldownSeconds) {
		if (player == null || status == null || status == TeleportStatus.SUCCESS || status == TeleportStatus.CANCELLED) {
			return;
		}
		switch (status) {
		case COOLDOWN -> player.sendSystemMessage(TranslationHelper.getTranslatedText(
				"commands.teleport_commands.common.cooldown", player,
				Component.literal(String.valueOf(cooldownSeconds))).withStyle(ChatFormatting.YELLOW), true);
		case NO_SAFE_POSITION -> sendUnsafeTeleportPrompt(player);
		case TARGET_UNAVAILABLE -> send(player, "commands.teleport_commands.common.worldNotFound",
				ChatFormatting.RED, ChatFormatting.BOLD);
		case PLAYER_DISCONNECTED -> {
		}
		default -> send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}
}
