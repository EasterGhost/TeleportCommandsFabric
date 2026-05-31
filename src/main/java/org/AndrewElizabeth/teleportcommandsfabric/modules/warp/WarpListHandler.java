package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageRequest;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointRows;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointFilter;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class WarpListHandler {
	private WarpListHandler() {
	}

	static int renderWarps(CommandSourceStack source, ServerPlayer player, WaypointListQuery query, boolean pagePicker) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		GlobalProfileManager globalManager = TeleportCommands.GLOBAL_PROFILE_MANAGER;
		if (globalManager == null || TeleportCommands.WAYPOINT_PAGES == null) {
			WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		UUID playerUuid = player.getUUID();
		CompletableFuture<List<NamedLocationView>> warpsFuture = globalManager.query(profile -> profile.getWarps());
		CompletableFuture<Set<UUID>> hiddenFuture = hiddenWarps(playerUuid);
		warpsFuture.thenCombine(hiddenFuture, WarpPageData::new)
				.whenComplete((data, throwable) -> player.level().getServer().execute(() -> {
					ServerPlayer currentPlayer = player.level().getServer().getPlayerList().getPlayer(playerUuid);
					if (currentPlayer == null) {
						return;
					}
					if (throwable != null) {
						ModConstants.LOGGER.error("Error while rendering warps.", throwable);
						WarpMessages.send(currentPlayer, "commands.teleport_commands.warps.error", ChatFormatting.RED,
								ChatFormatting.BOLD);
						return;
					}
					List<NamedLocationView> filtered = WaypointRows.filterAndSort(data.warps(), query);
					if (filtered.isEmpty()) {
						if (query.filter() instanceof WaypointFilter.Dimension dimension) {
							WarpMessages.sendNoWarpsInDimension(currentPlayer, dimension.dimensionId());
						} else {
							WarpMessages.send(currentPlayer, "commands.teleport_commands.warp.homeless", ChatFormatting.AQUA);
						}
						return;
					}
					WaypointPageRequest request = new WaypointPageRequest(WaypointPageKind.WARPS, data.warps(), data.hiddenWarpUuids(),
							null, isAdmin(source), query, language(currentPlayer));
					if (pagePicker) {
						currentPlayer.sendSystemMessage(TeleportCommands.WAYPOINT_PAGES.renderPagePicker(request), false);
						return;
					}
					TeleportCommands.WAYPOINT_PAGES.render(request).whenComplete((component, renderThrowable) -> player.level().getServer().execute(() -> {
						ServerPlayer target = player.level().getServer().getPlayerList().getPlayer(playerUuid);
						if (target == null) {
							return;
						}
						if (renderThrowable != null) {
							ModConstants.LOGGER.error("Error while rendering warps page.", renderThrowable);
							WarpMessages.send(target, "commands.teleport_commands.warps.error", ChatFormatting.RED,
									ChatFormatting.BOLD);
							return;
						}
						target.sendSystemMessage(component, false);
					}));
				}));
		return 0;
	}

	private static CompletableFuture<Set<UUID>> hiddenWarps(UUID playerUuid) {
		PlayerProfileManager manager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (manager == null) {
			return CompletableFuture.completedFuture(Set.of());
		}
		return manager.query(playerUuid, profile -> profile.getHiddenWarpUuids())
				.exceptionally(throwable -> Set.of());
	}

	private static boolean ensureEnabled(ServerPlayer player) {
		if (ConfigManager.query(config -> config.getWarp().isEnabled())) {
			return true;
		}
		WarpMessages.send(player, "commands.teleport_commands.warp.disabled", ChatFormatting.RED);
		return false;
	}

	private static boolean isAdmin(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
	}

	private static String language(ServerPlayer player) {
		return player.clientInformation().language().toLowerCase();
	}

	private record WarpPageData(List<NamedLocationView> warps, Set<UUID> hiddenWarpUuids) {
	}
}
