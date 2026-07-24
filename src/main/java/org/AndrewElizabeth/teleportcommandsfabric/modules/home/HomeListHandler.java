package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandAsyncSupport;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandReturns;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageRequest;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointFilterPickerKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointRenderMode;
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
		return renderHomes(player, query, pagePicker ? WaypointRenderMode.PAGE_PICKER : WaypointRenderMode.PAGE, null);
	}

	static int renderHomeFilterPicker(ServerPlayer player, WaypointListQuery query, WaypointFilterPickerKind pickerKind) {
		return renderHomes(player, query, pickerKind == WaypointFilterPickerKind.DIMENSION
				? WaypointRenderMode.DIMENSION_FILTER_PICKER
				: WaypointRenderMode.PREFIX_FILTER_PICKER, null);
	}

	static int renderHomeManage(ServerPlayer player, UUID waypointUuid, WaypointListQuery query, boolean deleteConfirmation) {
		return renderHomes(player, query, deleteConfirmation
				? WaypointRenderMode.DELETE_CONFIRMATION
				: WaypointRenderMode.MANAGE, waypointUuid);
	}

	private static int renderHomes(ServerPlayer player, WaypointListQuery query, WaypointRenderMode renderMode,
			UUID waypointUuid) {
		if (!ensureEnabled(player)) {
			return CommandReturns.FAILED;
		}
		PlayerProfileManager manager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (manager == null || TeleportCommands.WAYPOINT_PAGES == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}
		UUID playerUuid = player.getUUID();
		MinecraftServer server = player.level().getServer();
		CommandAsyncSupport.whenCompleteForPlayer(server, playerUuid,
				manager.query(playerUuid, profile -> new HomePageData(profile.getHomes(), profile.getDefaultHomeUuid())),
				(currentPlayer, data, throwable) -> {
					if (throwable != null) {
						ModConstants.LOGGER.error("Error while rendering homes.", throwable);
						HomeMessages.send(currentPlayer, "commands.teleport_commands.homes.error", ChatFormatting.RED,
								ChatFormatting.BOLD);
						return;
					}
					Set<UUID> publishedHomeUuids = TeleportCommands.SHARED_HOME_SERVICE == null
							? Set.of()
							: TeleportCommands.SHARED_HOME_SERVICE.publishedHomeUuids(playerUuid);
					WaypointPageRequest request = new WaypointPageRequest(WaypointPageKind.HOMES, data.homes(), Set.of(),
							data.defaultHomeUuid(), true, publishedHomeUuids, query, language(currentPlayer));
					if (renderMode == WaypointRenderMode.MANAGE || renderMode == WaypointRenderMode.DELETE_CONFIRMATION) {
						NamedLocationView location = WaypointRows.findByUuid(data.homes(), waypointUuid).orElse(null);
						if (location == null) {
							HomeMessages.send(currentPlayer, "commands.teleport_commands.home.notFound", ChatFormatting.RED);
							return;
						}
						currentPlayer.sendSystemMessage(renderMode == WaypointRenderMode.DELETE_CONFIRMATION
								? TeleportCommands.WAYPOINT_PAGES.renderDeleteConfirmation(request, location)
								: TeleportCommands.WAYPOINT_PAGES.renderManage(request, location), false);
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
							ModConstants.LOGGER.error("Error while rendering homes page.", renderThrowable);
							HomeMessages.send(target, "commands.teleport_commands.homes.error", ChatFormatting.RED,
									ChatFormatting.BOLD);
							return;
						}
						target.sendSystemMessage(component, false);
					});
				});
		return CommandReturns.ACCEPTED_ASYNC;
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
}
