package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageRequest;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointFilterPickerKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointRows;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointFilter;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

final class HomeListHandler {
	private HomeListHandler() {
	}

	static int renderHomes(ServerPlayer player, WaypointListQuery query, boolean pagePicker) {
		return renderHomes(player, query, pagePicker ? RenderMode.PAGE_PICKER : RenderMode.PAGE);
	}

	static int renderHomeFilterPicker(ServerPlayer player, WaypointListQuery query, WaypointFilterPickerKind pickerKind) {
		return renderHomes(player, query, pickerKind == WaypointFilterPickerKind.DIMENSION
				? RenderMode.DIMENSION_FILTER_PICKER
				: RenderMode.PREFIX_FILTER_PICKER);
	}

	private static int renderHomes(ServerPlayer player, WaypointListQuery query, RenderMode renderMode) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		PlayerProfileManager manager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (manager == null || TeleportCommands.WAYPOINT_PAGES == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		UUID playerUuid = player.getUUID();
		MinecraftServer server = player.level().getServer();
		manager.query(playerUuid, profile -> new HomePageData(profile.getHomes(), profile.getDefaultHomeUuid()))
				.whenComplete((data, throwable) -> server.execute(() -> {
					ServerPlayer currentPlayer = server.getPlayerList().getPlayer(playerUuid);
					if (currentPlayer == null) {
						return;
					}
					if (throwable != null) {
						ModConstants.LOGGER.error("Error while rendering homes.", throwable);
						HomeMessages.send(currentPlayer, "commands.teleport_commands.homes.error", ChatFormatting.RED,
								ChatFormatting.BOLD);
						return;
					}
					List<NamedLocationView> filtered = WaypointRows.filterAndSort(data.homes(), query);
					if (filtered.isEmpty()) {
						if (query.filter() instanceof WaypointFilter.Dimension dimension) {
							HomeMessages.sendNoHomesInDimension(currentPlayer, dimension.dimensionId());
						} else {
							HomeMessages.send(currentPlayer, "commands.teleport_commands.home.homeless", ChatFormatting.AQUA);
						}
						return;
					}
					WaypointPageRequest request = new WaypointPageRequest(WaypointPageKind.HOMES, data.homes(), Set.of(),
							data.defaultHomeUuid(), true, query, language(currentPlayer));
					if (renderMode == RenderMode.PAGE_PICKER) {
						currentPlayer.sendSystemMessage(TeleportCommands.WAYPOINT_PAGES.renderPagePicker(request), false);
						return;
					}
					if (renderMode == RenderMode.PREFIX_FILTER_PICKER || renderMode == RenderMode.DIMENSION_FILTER_PICKER) {
						WaypointFilterPickerKind pickerKind = renderMode == RenderMode.DIMENSION_FILTER_PICKER
								? WaypointFilterPickerKind.DIMENSION
								: WaypointFilterPickerKind.PREFIX;
						currentPlayer.sendSystemMessage(TeleportCommands.WAYPOINT_PAGES.renderFilterPicker(request, pickerKind),
								false);
						return;
					}
					TeleportCommands.WAYPOINT_PAGES.render(request).whenComplete((component, renderThrowable) -> server.execute(() -> {
						ServerPlayer target = server.getPlayerList().getPlayer(playerUuid);
						if (target == null) {
							return;
						}
						if (renderThrowable != null) {
							ModConstants.LOGGER.error("Error while rendering homes page.", renderThrowable);
							HomeMessages.send(target, "commands.teleport_commands.homes.error", ChatFormatting.RED,
									ChatFormatting.BOLD);
							return;
						}
						target.sendSystemMessage(component, false);
					}));
				}));
		return 0;
	}

	private static boolean ensureEnabled(ServerPlayer player) {
		if (ConfigManager.query(config -> config.getHome().isEnabled())) {
			return true;
		}
		HomeMessages.send(player, "commands.teleport_commands.home.disabled", ChatFormatting.RED);
		return false;
	}

	private static String language(ServerPlayer player) {
		return player.clientInformation().language().toLowerCase(Locale.ROOT);
	}

	private record HomePageData(List<NamedLocationView> homes, UUID defaultHomeUuid) {
	}

	private enum RenderMode {
		PAGE,
		PAGE_PICKER,
		PREFIX_FILTER_PICKER,
		DIMENSION_FILTER_PICKER
	}
}
