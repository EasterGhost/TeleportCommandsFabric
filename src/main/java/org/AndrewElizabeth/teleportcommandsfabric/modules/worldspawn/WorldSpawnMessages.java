package org.AndrewElizabeth.teleportcommandsfabric.modules.worldspawn;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.MessageSupport;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

public final class WorldSpawnMessages {
	private static final String FORCE_COMMAND = "worldspawn true";

	private WorldSpawnMessages() {
	}

	public static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		MessageSupport.send(player, key, formatting);
	}

	public static void sendDelayStart(ServerPlayer player, int delaySeconds) {
		MessageSupport.sendDelayStart(player, delaySeconds);
	}

	public static void sendStatus(ServerPlayer player, TeleportStatus status, int cooldownSeconds) {
		MessageSupport.sendTeleportStatus(player, status, cooldownSeconds, FORCE_COMMAND,
				"commands.teleport_commands.common.worldNotFound", ChatFormatting.RED, ChatFormatting.BOLD);
	}
}
