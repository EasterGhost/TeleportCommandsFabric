package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

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
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class WarpMapVisibilityHandler {
	private WarpMapVisibilityHandler() {
	}

	static int setPlayerVisibility(CommandContext<CommandSourceStack> context, boolean silent,
			WaypointListQuery query) {
		ServerPlayer player = player(context);
		if (player == null || !ensureEnabled(player, silent)) {
			return CommandReturns.FAILED;
		}

		String name = StringArgumentType.getString(context, "name");
		boolean visible = BoolArgumentType.getBool(context, "visible");
		UUID playerUuid = player.getUUID();
		WaypointMutationSupport.handle(player, updatePlayerVisibility(playerUuid, name, visible),
				"Error while updating warp map visibility.", () -> WaypointMapSyncEvents.markPlayerDirty(playerUuid),
				(currentPlayer, result, throwable) -> {
					if (throwable != null) {
						if (!silent) {
							WarpMessages.send(currentPlayer, "commands.teleport_commands.warps.error",
									ChatFormatting.RED, ChatFormatting.BOLD);
						}
						return;
					}
					if (!silent) {
						WarpMessages.sendPlayerVisibilityResult(currentPlayer, result, visible);
					} else if (query != null) {
						WarpListHandler.renderWarps(currentPlayer.createCommandSourceStack(), currentPlayer, query, false);
					}
				});
		return CommandReturns.ACCEPTED_ASYNC;
	}

	static int setGlobalVisibility(CommandContext<CommandSourceStack> context, WaypointListQuery query) {
		ServerPlayer player = player(context);
		if (player == null || !ensureEnabled(player, false)) {
			return CommandReturns.FAILED;
		}
		AsyncWaypointSource source = source();
		if (source == null) {
			WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}

		String name = StringArgumentType.getString(context, "name");
		boolean visible = BoolArgumentType.getBool(context, "visible");
		WaypointMutationSupport.handle(player, WaypointCrudService.updateVisibility(name, visible, source),
				"Error while updating global warp map visibility.", WarpMapVisibilityHandler::markGlobalDirty,
				(currentPlayer, result, throwable) -> {
					if (throwable != null) {
						WarpMessages.send(currentPlayer, "commands.teleport_commands.gwarpmap.error",
								ChatFormatting.RED, ChatFormatting.BOLD);
						return;
					}
					WarpMessages.sendGlobalVisibilityResult(currentPlayer, result, visible);
					if (query != null) {
						WarpListHandler.renderWarps(currentPlayer.createCommandSourceStack(), currentPlayer, query, false);
					}
				});
		return CommandReturns.ACCEPTED_ASYNC;
	}

	private static ServerPlayer player(CommandContext<CommandSourceStack> context) {
		try {
			return context.getSource().getPlayerOrException();
		} catch (Exception exception) {
			return null;
		}
	}

	private static CompletableFuture<WaypointOperationResult> updatePlayerVisibility(UUID playerUuid, String warpName,
			boolean visible) {
		GlobalProfileManager globalManager = TeleportCommands.GLOBAL_PROFILE_MANAGER;
		PlayerProfileManager playerManager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (globalManager == null || playerManager == null) {
			return CompletableFuture.completedFuture(WaypointOperationResult.INTERNAL_ERROR);
		}
		return globalManager.query(profile -> profile.getWarpByName(warpName))
				.thenCompose(warp -> {
					if (warp.isEmpty()) {
						return CompletableFuture.completedFuture(WaypointOperationResult.NOT_FOUND);
					}
					UUID warpUuid = warp.get().getUuid();
					return playerManager.mutate(playerUuid, profile -> {
						if (visible) {
							profile.showWarp(warpUuid);
						} else {
							profile.hideWarp(warpUuid);
						}
						return WaypointOperationResult.SUCCESS;
					});
				});
	}

	private static void markGlobalDirty() {
		if (TeleportCommands.WAYPOINT_PAGES != null) {
			TeleportCommands.WAYPOINT_PAGES.invalidateWarpCache();
		}
		WaypointMapSyncEvents.markAllDirty();
	}

	private static boolean ensureEnabled(ServerPlayer player, boolean silent) {
		if (ConfigManager.query(config -> config.getWarp().isEnabled())) {
			return true;
		}
		if (!silent) {
			WarpMessages.send(player, "commands.teleport_commands.warp.disabled", ChatFormatting.RED);
		}
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
