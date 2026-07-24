package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.AsyncWaypointSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.PlayerHomeSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointCrudService;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointMapSyncEvents;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandReturns;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.WaypointMutationSupport;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

final class HomeMapVisibilityHandler {
	private HomeMapVisibilityHandler() {
	}

	static int setVisibility(CommandContext<CommandSourceStack> context, boolean silent, WaypointListQuery query) {
		ServerPlayer player;
		try {
			player = context.getSource().getPlayerOrException();
		} catch (Exception exception) {
			return CommandReturns.FAILED;
		}
		if (!ensureEnabled(player, silent)) {
			return CommandReturns.FAILED;
		}
		AsyncWaypointSource source = source(player);
		if (source == null) {
			if (!silent) {
				HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			}
			return CommandReturns.FAILED;
		}

		String name = StringArgumentType.getString(context, "name");
		boolean visible = BoolArgumentType.getBool(context, "visible");
		MinecraftServer server = player.level().getServer();
		UUID playerUuid = player.getUUID();
		WaypointMutationSupport.handle(player, WaypointCrudService.updateVisibility(name, visible, source),
				"Error while updating home map visibility.", () -> {
					WaypointMapSyncEvents.markPlayerDirty(playerUuid);
					SharedHomePublicationHandler.onOwnerHomesChanged(server, playerUuid);
				}, (currentPlayer, result, throwable) -> {
					if (throwable != null) {
						if (!silent) {
							HomeMessages.send(currentPlayer, "commands.teleport_commands.homes.error",
									ChatFormatting.RED, ChatFormatting.BOLD);
						}
						return;
					}
					if (!silent) {
						HomeMessages.sendVisibilityResult(currentPlayer, result, visible);
					} else if (query != null) {
						HomeListHandler.renderHomes(currentPlayer, query, false);
					}
				});
		return CommandReturns.ACCEPTED_ASYNC;
	}

	private static boolean ensureEnabled(ServerPlayer player, boolean silent) {
		if (ConfigManager.query(config -> config.getHome().isEnabled())) {
			return true;
		}
		if (!silent) {
			HomeMessages.send(player, "commands.teleport_commands.home.disabled", ChatFormatting.RED);
		}
		return false;
	}

	private static AsyncWaypointSource source(ServerPlayer player) {
		PlayerProfileManager manager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (manager == null) {
			return null;
		}
		return new PlayerHomeSource(player.getUUID(), manager,
				() -> ConfigManager.query(config -> config.getHome().getPlayerMaximum()));
	}
}
