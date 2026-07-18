package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.MessageSupport;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class WarpMessages {
	private WarpMessages() {
	}

	static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		MessageSupport.send(player, key, formatting);
	}

	static void sendMaxReached(ServerPlayer player, int maxWarps) {
		if (player != null) {
			player.sendSystemMessage(MessageSupport.translated(player, "commands.teleport_commands.warp.max",
					Component.literal(String.valueOf(maxWarps))).withStyle(ChatFormatting.RED), true);
		}
	}

	static void sendNoWarpsInDimension(ServerPlayer player, String dimensionFilter) {
		if (player != null) {
			player.sendSystemMessage(MessageSupport.translated(player, "commands.teleport_commands.warp.noneInDimension",
					Component.literal(dimensionFilter).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.AQUA), true);
		}
	}

	static void sendDelayStart(ServerPlayer player, int delaySeconds) {
		MessageSupport.sendDelayStart(player, delaySeconds);
	}

	static void sendStatus(ServerPlayer player, TeleportStatus status, int cooldownSeconds, String forceCommand) {
		MessageSupport.sendTeleportStatus(player, status, cooldownSeconds, forceCommand,
				"commands.teleport_commands.warp.notFound", ChatFormatting.RED);
	}
}
