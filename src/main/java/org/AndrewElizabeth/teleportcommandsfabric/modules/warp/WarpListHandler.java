package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandAsyncSupport;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageRequest;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointFilterPickerKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointRenderMode;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointFilter;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class WarpListHandler {
	private WarpListHandler() {
	}

	static int renderWarps(CommandSourceStack source, ServerPlayer player, WaypointListQuery query, boolean pagePicker) {
		return renderWarps(source, player, query, pagePicker ? WaypointRenderMode.PAGE_PICKER : WaypointRenderMode.PAGE);
	}

	static int renderWarpFilterPicker(CommandSourceStack source, ServerPlayer player, WaypointListQuery query,
			WaypointFilterPickerKind pickerKind) {
		return renderWarps(source, player, query, pickerKind == WaypointFilterPickerKind.DIMENSION
				? WaypointRenderMode.DIMENSION_FILTER_PICKER
				: WaypointRenderMode.PREFIX_FILTER_PICKER);
	}

	private static int renderWarps(CommandSourceStack source, ServerPlayer player, WaypointListQuery query,
			WaypointRenderMode renderMode) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		GlobalProfileManager globalManager = TeleportCommands.GLOBAL_PROFILE_MANAGER;
		if (globalManager == null || TeleportCommands.WAYPOINT_PAGES == null) {
			WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		UUID playerUuid = player.getUUID();
		MinecraftServer server = player.level().getServer();
		boolean admin = isAdmin(source);
		CompletableFuture<List<NamedLocationView>> warpsFuture = globalManager.query(profile -> profile.getWarps());
		CompletableFuture<Set<UUID>> hiddenFuture = hiddenWarps(playerUuid);
		CommandAsyncSupport.whenCompleteForPlayer(server, playerUuid,
				warpsFuture.thenCombine(hiddenFuture, WarpPageData::new), (currentPlayer, data, throwable) -> {
					if (throwable != null) {
						ModConstants.LOGGER.error("Error while rendering warps.", throwable);
						WarpMessages.send(currentPlayer, "commands.teleport_commands.warps.error", ChatFormatting.RED,
								ChatFormatting.BOLD);
						return;
					}
					WaypointPageRequest request = new WaypointPageRequest(WaypointPageKind.WARPS, data.warps(), data.hiddenWarpUuids(),
							null, admin, query, language(currentPlayer));
					List<NamedLocationView> filtered = TeleportCommands.WAYPOINT_PAGES.filteredRows(request);
					if (filtered.isEmpty()) {
						if (query.filter() instanceof WaypointFilter.Dimension dimension) {
							WarpMessages.sendNoWarpsInDimension(currentPlayer, dimension.dimensionId());
						} else {
							WarpMessages.send(currentPlayer, "commands.teleport_commands.warp.homeless", ChatFormatting.AQUA);
						}
						return;
					}
					if (renderMode == WaypointRenderMode.PAGE_PICKER) {
						currentPlayer.sendSystemMessage(TeleportCommands.WAYPOINT_PAGES.renderPagePicker(request), false);
						return;
					}
					if (renderMode == WaypointRenderMode.PREFIX_FILTER_PICKER || renderMode == WaypointRenderMode.DIMENSION_FILTER_PICKER) {
						WaypointFilterPickerKind pickerKind = renderMode == WaypointRenderMode.DIMENSION_FILTER_PICKER
								? WaypointFilterPickerKind.DIMENSION
								: WaypointFilterPickerKind.PREFIX;
						currentPlayer.sendSystemMessage(TeleportCommands.WAYPOINT_PAGES.renderFilterPicker(request, pickerKind),
								false);
						return;
					}
					CommandAsyncSupport.whenCompleteForPlayer(server, playerUuid,
							TeleportCommands.WAYPOINT_PAGES.render(request), (target, component, renderThrowable) -> {
						if (renderThrowable != null) {
							ModConstants.LOGGER.error("Error while rendering warps page.", renderThrowable);
							WarpMessages.send(target, "commands.teleport_commands.warps.error", ChatFormatting.RED,
									ChatFormatting.BOLD);
							return;
						}
						target.sendSystemMessage(component, false);
					});
				});
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
		return player.clientInformation().language().toLowerCase(Locale.ROOT);
	}

	private record WarpPageData(List<NamedLocationView> warps, Set<UUID> hiddenWarpUuids) {
	}
}
