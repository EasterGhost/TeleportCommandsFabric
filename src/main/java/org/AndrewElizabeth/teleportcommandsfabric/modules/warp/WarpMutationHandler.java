package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.AsyncWaypointSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.GlobalWarpSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointCrudService;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointMapSyncEvents;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointOperationResult;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandReturns;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.WaypointMutationSupport;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

final class WarpMutationHandler {
	private WarpMutationHandler() {
	}

	static int setWarp(ServerPlayer player, String name) {
		return executeMutation(player, source -> WaypointCrudService.add(player, name, source),
				"commands.teleport_commands.warp.set", "Error while setting a warp.");
	}

	static int updateWarp(ServerPlayer player, String name) {
		return executeMutation(player, source -> WaypointCrudService.update(player, name, source),
				"commands.teleport_commands.warp.update", "Error while updating a warp.");
	}

	static int updateWarpFromManage(ServerPlayer player, UUID waypointUuid, WaypointListQuery query) {
		return executeMutation(player, source -> WaypointCrudService.update(player, waypointUuid, source),
				"commands.teleport_commands.warp.update", "Error while updating a warp.",
				currentPlayer -> WarpListHandler.renderWarpManage(currentPlayer.createCommandSourceStack(), currentPlayer,
						waypointUuid, query, false));
	}

	static int deleteWarp(ServerPlayer player, String name) {
		return executeMutation(player, source -> WaypointCrudService.delete(name, source),
				"commands.teleport_commands.warp.delete", "Error while deleting a warp.");
	}

	static int deleteWarpFromManage(ServerPlayer player, UUID waypointUuid, WaypointListQuery query) {
		return executeMutation(player, source -> WaypointCrudService.delete(waypointUuid, source),
				"commands.teleport_commands.warp.delete", "Error while deleting a warp.",
				currentPlayer -> WarpListHandler.renderWarps(currentPlayer.createCommandSourceStack(), currentPlayer, query, false));
	}

	static int renameWarp(ServerPlayer player, String oldName, String newName) {
		return executeMutation(player, source -> WaypointCrudService.rename(oldName, newName, source),
				"commands.teleport_commands.warp.rename", "Error while renaming a warp.");
	}

	private static int executeMutation(ServerPlayer player,
			Function<AsyncWaypointSource, CompletableFuture<WaypointOperationResult>> mutation,
			String successKey, String logMessage) {
		return executeMutation(player, mutation, successKey, logMessage, null);
	}

	private static int executeMutation(ServerPlayer player,
			Function<AsyncWaypointSource, CompletableFuture<WaypointOperationResult>> mutation,
			String successKey, String logMessage, Consumer<ServerPlayer> successAction) {
		if (!ensureEnabled(player)) {
			return CommandReturns.FAILED;
		}
		AsyncWaypointSource source = source();
		if (source == null) {
			WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}

		int maxWarps = ConfigManager.query(config -> config.getWarp().getMaximum());
		WaypointMutationSupport.handle(player, mutation.apply(source), logMessage, WarpMutationHandler::markGlobalDirty,
				(currentPlayer, result, throwable) -> {
					if (throwable != null) {
						WarpMessages.send(currentPlayer, "commands.teleport_commands.common.error", ChatFormatting.RED,
								ChatFormatting.BOLD);
						return;
					}
					WarpMessages.sendMutationResult(currentPlayer, result, successKey, maxWarps);
					if (result == WaypointOperationResult.SUCCESS && successAction != null) {
						successAction.accept(currentPlayer);
					}
				});
		return CommandReturns.ACCEPTED_ASYNC;
	}

	private static void markGlobalDirty() {
		if (TeleportCommands.WAYPOINT_PAGES != null) {
			TeleportCommands.WAYPOINT_PAGES.invalidateWarpCache();
		}
		WaypointMapSyncEvents.markAllDirty();
	}

	private static boolean ensureEnabled(ServerPlayer player) {
		if (ConfigManager.query(config -> config.getWarp().isEnabled())) {
			return true;
		}
		WarpMessages.send(player, "commands.teleport_commands.warp.disabled", ChatFormatting.RED);
		return false;
	}

	private static AsyncWaypointSource source() {
		GlobalProfileManager manager = TeleportCommands.GLOBAL_PROFILE_MANAGER;
		if (manager == null) {
			return null;
		}
		return new GlobalWarpSource(manager, () -> ConfigManager.query(config -> config.getWarp().getMaximum()));
	}
}
