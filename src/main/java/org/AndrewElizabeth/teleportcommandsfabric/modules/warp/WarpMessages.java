package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointOperationResult;
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

	static void sendMutationResult(ServerPlayer player, WaypointOperationResult result, String successKey, int maxWarps) {
		switch (result) {
		case SUCCESS -> send(player, successKey, ChatFormatting.GREEN);
		case SAME_LOCATION -> send(player, "commands.teleport_commands.warp.updateSame", ChatFormatting.AQUA);
		case NOT_FOUND -> send(player, "commands.teleport_commands.warp.notFound", ChatFormatting.RED);
		case ALREADY_EXISTS -> send(player, "commands.teleport_commands.warp.exists", ChatFormatting.RED);
		case LIMIT_REACHED -> sendMaxReached(player, maxWarps);
		default -> send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	static void sendPlayerVisibilityResult(ServerPlayer player, WaypointOperationResult result, boolean visible) {
		switch (result) {
		case SUCCESS -> send(player, visible
				? "commands.teleport_commands.warp.playerMapShown"
				: "commands.teleport_commands.warp.playerMapHidden", ChatFormatting.GREEN);
		case NOT_FOUND -> send(player, "commands.teleport_commands.warp.notFound", ChatFormatting.RED);
		default -> send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	static void sendGlobalVisibilityResult(ServerPlayer player, WaypointOperationResult result, boolean visible) {
		switch (result) {
		case SUCCESS -> send(player, visible
				? "commands.teleport_commands.warp.mapShown"
				: "commands.teleport_commands.warp.mapHidden", ChatFormatting.GREEN);
		case NOT_FOUND -> send(player, "commands.teleport_commands.warp.notFound", ChatFormatting.RED);
		default -> send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}
}
