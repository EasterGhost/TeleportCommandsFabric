package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointOperationResult;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.MessageSupport;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class HomeMessages {
	private HomeMessages() {
	}

	static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		MessageSupport.send(player, key, formatting);
	}

	static void sendMaxReached(ServerPlayer player, int maxHomes) {
		if (player != null) {
			player.sendSystemMessage(MessageSupport.translated(player, "commands.teleport_commands.home.max",
					Component.literal(String.valueOf(maxHomes))).withStyle(ChatFormatting.RED), true);
		}
	}

	static void sendNoHomesInDimension(ServerPlayer player, String dimensionFilter) {
		if (player != null) {
			player.sendSystemMessage(MessageSupport.translated(player, "commands.teleport_commands.home.noneInDimension",
					Component.literal(dimensionFilter).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.AQUA), true);
		}
	}

	static void sendDelayStart(ServerPlayer player, int delaySeconds) {
		MessageSupport.sendDelayStart(player, delaySeconds);
	}

	static void sendStatus(ServerPlayer player, TeleportStatus status, int cooldownSeconds, String forceCommand) {
		MessageSupport.sendTeleportStatus(player, status, cooldownSeconds, forceCommand,
				"commands.teleport_commands.home.notFound", ChatFormatting.AQUA);
	}

	static void sendMutationResult(ServerPlayer player, WaypointOperationResult result, String successKey, int maxHomes) {
		switch (result) {
		case SUCCESS -> send(player, successKey, ChatFormatting.GREEN);
		case SAME_LOCATION -> send(player, "commands.teleport_commands.home.updateSame", ChatFormatting.AQUA);
		case SAME_DEFAULT -> send(player, "commands.teleport_commands.home.defaultSame", ChatFormatting.AQUA);
		case NOT_FOUND -> send(player, "commands.teleport_commands.home.notFound", ChatFormatting.RED);
		case ALREADY_EXISTS -> send(player, "commands.teleport_commands.home.exists", ChatFormatting.RED);
		case LIMIT_REACHED -> sendMaxReached(player, maxHomes);
		case TEMP_HOME_EXISTS -> send(player, "commands.teleport_commands.home.tempExists", ChatFormatting.RED);
		case CANNOT_BE_DEFAULT -> send(player, "commands.teleport_commands.home.defaultTemporary", ChatFormatting.RED);
		case DEFAULT_NOT_SUPPORTED -> send(player, "commands.teleport_commands.common.error", ChatFormatting.RED,
				ChatFormatting.BOLD);
		default -> send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	static void sendVisibilityResult(ServerPlayer player, WaypointOperationResult result, boolean visible) {
		switch (result) {
		case SUCCESS -> send(player, visible
				? "commands.teleport_commands.home.mapShown"
				: "commands.teleport_commands.home.mapHidden", ChatFormatting.GREEN);
		case NOT_FOUND -> send(player, "commands.teleport_commands.home.notFound", ChatFormatting.RED);
		default -> send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}
}
