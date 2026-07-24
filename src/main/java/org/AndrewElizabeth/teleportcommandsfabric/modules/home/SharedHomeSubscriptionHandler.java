package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointMapSyncEvents;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeKey;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeResolver;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeService;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandAsyncSupport;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandReturns;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

final class SharedHomeSubscriptionHandler {
	private SharedHomeSubscriptionHandler() {
	}

	static int subscribe(ServerPlayer player, SharedHomeKey key) {
		if (!ensureAvailable(player)) {
			return CommandReturns.FAILED;
		}
		MinecraftServer server = player.level().getServer();
		UUID playerUuid = player.getUUID();
		CommandAsyncSupport.whenCompleteForPlayer(server, playerUuid,
				SharedHomeResolver.resolve(key, TeleportCommands.PLAYER_PROFILE_MANAGER),
				(currentPlayer, home, throwable) -> {
			if (throwable != null) {
				ModConstants.LOGGER.error("Failed to resolve a shared home subscription.", throwable);
				SharedHomeMessages.send(currentPlayer, "commands.teleport_commands.common.error", ChatFormatting.RED,
						ChatFormatting.BOLD);
				return;
			}
			if (home == null || home.isEmpty() || home.get().isTemporary()) {
				SharedHomePublicationHandler.invalidateMissing(key);
				SharedHomeMessages.send(currentPlayer, "commands.teleport_commands.sharedhome.unavailable", ChatFormatting.RED);
				return;
			}
			handleSubscribe(currentPlayer, key);
		});
		return CommandReturns.ACCEPTED_ASYNC;
	}

	static int unsubscribe(ServerPlayer player, SharedHomeKey key, WaypointListQuery query) {
		if (!ensureAvailable(player)) {
			return CommandReturns.FAILED;
		}
		if (!TeleportCommands.SHARED_HOME_SERVICE.unsubscribe(player.getUUID(), key)) {
			SharedHomeMessages.send(player, "commands.teleport_commands.sharedhome.notSubscribed", ChatFormatting.RED);
			return CommandReturns.FAILED;
		}
		WaypointMapSyncEvents.markPlayerDirty(player.getUUID());
		SharedHomeMessages.send(player, "commands.teleport_commands.sharedhome.unsubscribed", ChatFormatting.GREEN);
		SharedHomeListHandler.renderSharedHomes(player, query, false);
		return CommandReturns.COMPLETED_SYNC;
	}

	static int setMapVisible(ServerPlayer player, SharedHomeKey key, boolean visible, WaypointListQuery query) {
		if (!ensureAvailable(player)) {
			return CommandReturns.FAILED;
		}
		if (!TeleportCommands.SHARED_HOME_SERVICE.setMapVisible(player.getUUID(), key, visible)) {
			SharedHomeMessages.send(player, "commands.teleport_commands.sharedhome.notSubscribed", ChatFormatting.RED);
			return CommandReturns.FAILED;
		}
		WaypointMapSyncEvents.markPlayerDirty(player.getUUID());
		SharedHomeListHandler.renderSharedHomes(player, query, false);
		return CommandReturns.COMPLETED_SYNC;
	}

	static int setMapVisible(ServerPlayer player, SharedHomeKey key, boolean visible) {
		if (!ensureAvailable(player)) {
			return CommandReturns.FAILED;
		}
		if (!TeleportCommands.SHARED_HOME_SERVICE.setMapVisible(player.getUUID(), key, visible)) {
			return CommandReturns.FAILED;
		}
		WaypointMapSyncEvents.markPlayerDirty(player.getUUID());
		return CommandReturns.COMPLETED_SYNC;
	}

	private static void handleSubscribe(ServerPlayer player, SharedHomeKey key) {
		SharedHomeService.SubscriptionStatus result = TeleportCommands.SHARED_HOME_SERVICE.subscribe(player.getUUID(), key);
		switch (result) {
		case SUBSCRIBED -> {
			WaypointMapSyncEvents.markPlayerDirty(player.getUUID());
			SharedHomeMessages.send(player, "commands.teleport_commands.sharedhome.subscribed", ChatFormatting.GREEN);
		}
		case ALREADY_SUBSCRIBED -> SharedHomeMessages.send(player,
				"commands.teleport_commands.sharedhome.alreadySubscribed", ChatFormatting.AQUA);
		case SELF -> SharedHomeMessages.send(player, "commands.teleport_commands.sharedhome.self", ChatFormatting.RED);
		case NOT_FOUND -> SharedHomeMessages.send(player, "commands.teleport_commands.sharedhome.unavailable", ChatFormatting.RED);
		}
	}

	private static boolean ensureAvailable(ServerPlayer player) {
		if (!ConfigManager.query(config -> config.getHome().isEnabled())) {
			HomeMessages.send(player, "commands.teleport_commands.home.disabled", ChatFormatting.RED);
			return false;
		}
		if (TeleportCommands.SHARED_HOME_SERVICE == null || TeleportCommands.PLAYER_PROFILE_MANAGER == null) {
			SharedHomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return false;
		}
		return true;
	}
}
