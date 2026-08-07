package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeResolver;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeView;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandAsyncSupport;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandReturns;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.MessageSupport;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointFilterPickerKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageRequest;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointRenderMode;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointRows;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointFilter;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

final class SharedHomeListHandler {
	private SharedHomeListHandler() {
	}

	static int renderSharedHomes(ServerPlayer player, WaypointListQuery query, boolean pagePicker) {
		return renderSharedHomes(player, query, pagePicker ? WaypointRenderMode.PAGE_PICKER : WaypointRenderMode.PAGE);
	}

	static int renderFilterPicker(ServerPlayer player, WaypointListQuery query, WaypointFilterPickerKind pickerKind) {
		return renderSharedHomes(player, query, pickerKind == WaypointFilterPickerKind.DIMENSION
				? WaypointRenderMode.DIMENSION_FILTER_PICKER
				: WaypointRenderMode.PREFIX_FILTER_PICKER);
	}

	private static int renderSharedHomes(ServerPlayer player, WaypointListQuery query, WaypointRenderMode renderMode) {
		if (!ensureAvailable(player)) {
			return CommandReturns.FAILED;
		}
		UUID playerUuid = player.getUUID();
		MinecraftServer server = player.level().getServer();
		CommandAsyncSupport.whenCompleteForPlayer(server, playerUuid,
				SharedHomeResolver.resolveSubscriptions(playerUuid, TeleportCommands.SHARED_HOME_SERVICE,
						TeleportCommands.PLAYER_PROFILE_MANAGER, server),
				(currentPlayer, homes, throwable) -> {
			if (throwable != null) {
				ModConstants.LOGGER.error("Failed to render shared homes.", throwable);
				SharedHomeMessages.send(currentPlayer, "commands.teleport_commands.sharedhomes.error",
						ChatFormatting.RED, ChatFormatting.BOLD);
				return;
			}
			renderResolved(currentPlayer, homes, query, renderMode);
		});
		return CommandReturns.ACCEPTED_ASYNC;
	}

	private static void renderResolved(ServerPlayer player, List<SharedHomeView> homes, WaypointListQuery query,
			WaypointRenderMode renderMode) {
		List<NamedLocationView> locations = homes.stream()
				.map(NamedLocationView.class::cast)
				.toList();
		WaypointPageRequest request = new WaypointPageRequest(WaypointPageKind.SHARED_HOMES, locations, Set.of(),
				null, false, query, language(player));
		List<NamedLocationView> filtered = WaypointRows.filterAndSort(locations, query);
		if (filtered.isEmpty()) {
			if (query.filter() instanceof WaypointFilter.Dimension dimension) {
				player.sendSystemMessage(MessageSupport.translated(player,
						"commands.teleport_commands.sharedhomes.noneInDimension",
						Component.literal(dimension.dimensionId())).withStyle(ChatFormatting.AQUA), true);
			} else {
				SharedHomeMessages.send(player, "commands.teleport_commands.sharedhomes.none", ChatFormatting.AQUA);
			}
			return;
		}
		if (renderMode == WaypointRenderMode.PAGE_PICKER) {
			player.sendSystemMessage(TeleportCommands.WAYPOINT_PAGES.renderPagePicker(request), false);
			return;
		}
		if (renderMode == WaypointRenderMode.PREFIX_FILTER_PICKER
				|| renderMode == WaypointRenderMode.DIMENSION_FILTER_PICKER) {
			WaypointFilterPickerKind pickerKind = renderMode == WaypointRenderMode.DIMENSION_FILTER_PICKER
					? WaypointFilterPickerKind.DIMENSION
					: WaypointFilterPickerKind.PREFIX;
			player.sendSystemMessage(TeleportCommands.WAYPOINT_PAGES.renderFilterPicker(request, pickerKind), false);
			return;
		}

		MinecraftServer server = player.level().getServer();
		UUID playerUuid = player.getUUID();
		CommandAsyncSupport.whenCompleteForPlayer(server, playerUuid, TeleportCommands.WAYPOINT_PAGES.render(request),
				(currentPlayer, component, throwable) -> {
			if (throwable != null) {
				ModConstants.LOGGER.error("Failed to render shared homes page.", throwable);
				SharedHomeMessages.send(currentPlayer, "commands.teleport_commands.sharedhomes.error",
						ChatFormatting.RED, ChatFormatting.BOLD);
				return;
			}
			currentPlayer.sendSystemMessage(component, false);
		});
	}

	private static boolean ensureAvailable(ServerPlayer player) {
		if (!ConfigManager.query(config -> config.getHome().isEnabled())) {
			HomeMessages.send(player, "commands.teleport_commands.home.disabled", ChatFormatting.RED);
			return false;
		}
		if (TeleportCommands.SHARED_HOME_SERVICE == null || TeleportCommands.PLAYER_PROFILE_MANAGER == null
				|| TeleportCommands.WAYPOINT_PAGES == null) {
			SharedHomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return false;
		}
		return true;
	}

	private static String language(ServerPlayer player) {
		return player.clientInformation().language().toLowerCase(Locale.ROOT);
	}
}
