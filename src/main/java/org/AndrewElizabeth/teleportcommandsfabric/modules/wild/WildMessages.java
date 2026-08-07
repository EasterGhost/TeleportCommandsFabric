package org.AndrewElizabeth.teleportcommandsfabric.modules.wild;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.MessageSupport;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

final class WildMessages {
	private WildMessages() {
	}

	static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		MessageSupport.send(player, key, formatting);
	}

	static void sendDelayStart(ServerPlayer player, int delaySeconds) {
		MessageSupport.sendDelayStart(player, delaySeconds);
	}

	static void sendStatus(ServerPlayer player, TeleportStatus status, int cooldownSeconds) {
		MessageSupport.sendTeleportStatus(player, status, cooldownSeconds, null,
				"commands.teleport_commands.wild.unsupportedDimension", ChatFormatting.RED);
	}
}
