package org.AndrewElizabeth.teleportcommandsfabric.modules.common;

import org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public final class MessageSupport {
	private MessageSupport() {
	}

	public static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		if (player != null) {
			player.sendSystemMessage(translated(player, key).withStyle(formatting), true);
		}
	}

	public static void sendDelayStart(ServerPlayer player, int delaySeconds) {
		if (player != null) {
			player.sendSystemMessage(translated(player, "commands.teleport_commands.common.delayStart",
					Component.literal(String.valueOf(delaySeconds))).withStyle(ChatFormatting.AQUA), true);
		}
	}

	public static void sendCooldown(ServerPlayer player, int cooldownSeconds) {
		if (player != null) {
			player.sendSystemMessage(translated(player, "commands.teleport_commands.common.cooldown",
					Component.literal(String.valueOf(cooldownSeconds))).withStyle(ChatFormatting.YELLOW), true);
		}
	}

	public static void sendUnsafeTeleportPrompt(ServerPlayer player, String command) {
		if (player == null) {
			return;
		}
		player.sendSystemMessage(Component.empty()
				.append(translated(player, "commands.teleport_commands.common.noSafeLocation")
						.withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
				.append("\n")
				.append(translated(player, "commands.teleport_commands.common.safetyIsForLosers")
						.withStyle(ChatFormatting.WHITE))
				.append("\n")
				.append(translated(player, "commands.teleport_commands.common.forceTeleport")
						.withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)
						.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(command))))
				.append("\n"), false);
	}

	public static MutableComponent translated(ServerPlayer player, String key, MutableComponent... args) {
		return TranslationHelper.getTranslatedText(key, player, args);
	}
}
