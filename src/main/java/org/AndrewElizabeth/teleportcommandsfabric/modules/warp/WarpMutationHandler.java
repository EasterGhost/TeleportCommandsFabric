package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.AsyncWaypointSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.GlobalWarpSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointMapSyncEvents;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointCrudService;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointOperationResult;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class WarpMutationHandler {
	private WarpMutationHandler() {
	}

	static int setWarp(ServerPlayer player, String name) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		AsyncWaypointSource source = source();
		if (source == null) {
			WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		handleGlobalMutationResult(player, WaypointCrudService.add(player, name, source),
				"commands.teleport_commands.warp.set", "Error while setting a warp.");
		return 0;
	}

	static int updateWarp(ServerPlayer player, String name) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		AsyncWaypointSource source = source();
		if (source == null) {
			WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		handleGlobalMutationResult(player, WaypointCrudService.update(player, name, source),
				"commands.teleport_commands.warp.update", "Error while updating a warp.");
		return 0;
	}

	static int deleteWarp(ServerPlayer player, String name) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		AsyncWaypointSource source = source();
		if (source == null) {
			WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		handleGlobalMutationResult(player, WaypointCrudService.delete(name, source),
				"commands.teleport_commands.warp.delete", "Error while deleting a warp.");
		return 0;
	}

	static int renameWarp(ServerPlayer player, String oldName, String newName) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		AsyncWaypointSource source = source();
		if (source == null) {
			WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		handleGlobalMutationResult(player, WaypointCrudService.rename(oldName, newName, source),
				"commands.teleport_commands.warp.rename", "Error while renaming a warp.");
		return 0;
	}

	static int setPlayerMapVisibility(CommandContext<CommandSourceStack> context, boolean silent,
			WaypointListQuery query) {
		ServerPlayer player;
		try {
			player = context.getSource().getPlayerOrException();
		} catch (Exception exception) {
			return 1;
		}
		if (!ensureEnabled(player, silent)) {
			return 1;
		}
		String name = StringArgumentType.getString(context, "name");
		boolean visible = BoolArgumentType.getBool(context, "visible");
		MinecraftServer server = player.level().getServer();
		UUID playerUuid = player.getUUID();
		updatePlayerMapVisibility(player, name, visible).whenComplete((result, throwable) -> server.execute(() -> {
			ServerPlayer currentPlayer = server.getPlayerList().getPlayer(playerUuid);
			if (currentPlayer == null) {
				return;
			}
			if (throwable != null) {
				ModConstants.LOGGER.error("Error while updating warp map visibility.", throwable);
				if (!silent) {
					WarpMessages.send(currentPlayer, "commands.teleport_commands.warps.error", ChatFormatting.RED, ChatFormatting.BOLD);
				}
				return;
			}
			if (result == WaypointOperationResult.SUCCESS) {
				WaypointMapSyncEvents.markPlayerDirty(playerUuid);
			}
			if (!silent) {
				sendPlayerVisibilityResult(currentPlayer, result, visible);
				return;
			}
			if (query != null) {
				WarpListHandler.renderWarps(context.getSource(), currentPlayer, query, false);
			}
		}));
		return 0;
	}

	static int setGlobalMapVisibility(CommandContext<CommandSourceStack> context, WaypointListQuery query) {
		ServerPlayer player;
		try {
			player = context.getSource().getPlayerOrException();
		} catch (Exception exception) {
			return 1;
		}
		if (!ensureEnabled(player)) {
			return 1;
		}
		AsyncWaypointSource source = source();
		if (source == null) {
			WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		String name = StringArgumentType.getString(context, "name");
		boolean visible = BoolArgumentType.getBool(context, "visible");
		MinecraftServer server = player.level().getServer();
		UUID playerUuid = player.getUUID();
		WaypointCrudService.updateVisibility(name, visible, source).whenComplete((result, throwable) -> server.execute(() -> {
			ServerPlayer currentPlayer = server.getPlayerList().getPlayer(playerUuid);
			if (currentPlayer == null) {
				return;
			}
			if (throwable != null) {
				ModConstants.LOGGER.error("Error while updating global warp map visibility.", throwable);
				WarpMessages.send(currentPlayer, "commands.teleport_commands.gwarpmap.error", ChatFormatting.RED, ChatFormatting.BOLD);
				return;
			}
			if (result == WaypointOperationResult.SUCCESS && TeleportCommands.WAYPOINT_PAGES != null) {
				TeleportCommands.WAYPOINT_PAGES.invalidateWarpCache();
			}
			if (result == WaypointOperationResult.SUCCESS) {
				WaypointMapSyncEvents.markAllDirty();
			}
			sendGlobalVisibilityResult(currentPlayer, result, visible);
			if (query != null) {
				WarpListHandler.renderWarps(context.getSource(), currentPlayer, query, false);
			}
		}));
		return 0;
	}

	private static void handleGlobalMutationResult(ServerPlayer player, CompletableFuture<WaypointOperationResult> future,
			String successKey, String logMessage) {
		int maxWarps = ConfigManager.query(config -> config.getWarp().getMaximum());
		MinecraftServer server = player.level().getServer();
		UUID playerUuid = player.getUUID();
		future.whenComplete((result, throwable) -> server.execute(() -> {
			ServerPlayer currentPlayer = server.getPlayerList().getPlayer(playerUuid);
			if (currentPlayer == null) {
				return;
			}
			if (throwable != null) {
				ModConstants.LOGGER.error(logMessage, throwable);
				WarpMessages.send(currentPlayer, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
				return;
			}
			if (result == WaypointOperationResult.SUCCESS && TeleportCommands.WAYPOINT_PAGES != null) {
				TeleportCommands.WAYPOINT_PAGES.invalidateWarpCache();
			}
			if (result == WaypointOperationResult.SUCCESS) {
				WaypointMapSyncEvents.markAllDirty();
			}
			sendMutationResult(currentPlayer, result, successKey, maxWarps);
		}));
	}

	private static void sendMutationResult(ServerPlayer player, WaypointOperationResult result, String successKey, int maxWarps) {
		switch (result) {
		case SUCCESS -> WarpMessages.send(player, successKey, ChatFormatting.GREEN);
		case SAME_LOCATION -> WarpMessages.send(player, "commands.teleport_commands.warp.updateSame", ChatFormatting.AQUA);
		case NOT_FOUND -> WarpMessages.send(player, "commands.teleport_commands.warp.notFound", ChatFormatting.RED);
		case ALREADY_EXISTS -> WarpMessages.send(player, "commands.teleport_commands.warp.exists", ChatFormatting.RED);
		case LIMIT_REACHED -> WarpMessages.sendMaxReached(player, maxWarps);
		default -> WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	private static void sendPlayerVisibilityResult(ServerPlayer player, WaypointOperationResult result, boolean visible) {
		switch (result) {
		case SUCCESS -> WarpMessages.send(player, visible
				? "commands.teleport_commands.warp.playerMapShown"
				: "commands.teleport_commands.warp.playerMapHidden", ChatFormatting.GREEN);
		case NOT_FOUND -> WarpMessages.send(player, "commands.teleport_commands.warp.notFound", ChatFormatting.RED);
		default -> WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	private static void sendGlobalVisibilityResult(ServerPlayer player, WaypointOperationResult result, boolean visible) {
		switch (result) {
		case SUCCESS -> WarpMessages.send(player, visible
				? "commands.teleport_commands.warp.mapShown"
				: "commands.teleport_commands.warp.mapHidden", ChatFormatting.GREEN);
		case NOT_FOUND -> WarpMessages.send(player, "commands.teleport_commands.warp.notFound", ChatFormatting.RED);
		default -> WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	private static CompletableFuture<WaypointOperationResult> updatePlayerMapVisibility(ServerPlayer player, String warpName,
			boolean visible) {
		GlobalProfileManager globalManager = TeleportCommands.GLOBAL_PROFILE_MANAGER;
		PlayerProfileManager playerManager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (globalManager == null || playerManager == null) {
			return CompletableFuture.completedFuture(WaypointOperationResult.INTERNAL_ERROR);
		}
		UUID playerUuid = player.getUUID();
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

	private static boolean ensureEnabled(ServerPlayer player) {
		return ensureEnabled(player, false);
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
