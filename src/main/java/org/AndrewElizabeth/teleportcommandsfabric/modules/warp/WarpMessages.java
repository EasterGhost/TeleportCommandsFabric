package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.MessageSupport;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class WarpMessages {
	private WarpMessages() {
	}

	public static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		MessageSupport.send(player, key, formatting);
	}

	public static void sendMaxReached(ServerPlayer player, int maxWarps) {
		if (player != null) {
			player.sendSystemMessage(MessageSupport.translated(player, "commands.teleport_commands.warp.max",
					Component.literal(String.valueOf(maxWarps))).withStyle(ChatFormatting.RED), true);
		}
	}

	public static void sendNoWarpsInDimension(ServerPlayer player, String dimensionFilter) {
		if (player != null) {
			player.sendSystemMessage(MessageSupport.translated(player, "commands.teleport_commands.warp.noneInDimension",
					Component.literal(dimensionFilter).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.AQUA), true);
		}
	}

	public static void sendDelayStart(ServerPlayer player, int delaySeconds) {
		MessageSupport.sendDelayStart(player, delaySeconds);
	}

	public static void sendUnsafeTeleportPrompt(ServerPlayer player, String command) {
		MessageSupport.sendUnsafeTeleportPrompt(player, command);
	}

	public static void sendStatus(ServerPlayer player, TeleportStatus status, int cooldownSeconds, String forceCommand) {
		if (player == null || status == null || status == TeleportStatus.SUCCESS || status == TeleportStatus.CANCELLED) {
			return;
		}
		switch (status) {
		case COOLDOWN -> MessageSupport.sendCooldown(player, cooldownSeconds);
		case NO_SAFE_POSITION -> sendUnsafeTeleportPrompt(player, forceCommand);
		case TARGET_UNAVAILABLE -> send(player, "commands.teleport_commands.warp.notFound", ChatFormatting.RED);
		case PLAYER_DISCONNECTED -> {
		}
		default -> send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}
}
