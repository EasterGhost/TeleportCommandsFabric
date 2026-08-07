package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.AsyncWaypointSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.PlayerHomeSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointCrudService;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointMapSyncEvents;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointOperationResult;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandReturns;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.WaypointMutationSupport;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TimeUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

final class HomeMutationHandler {
	private HomeMutationHandler() {
	}

	static int setHome(ServerPlayer player, String name, boolean temporary) {
		return executeMutation(player, source -> temporary
				? WaypointCrudService.addTemporary(player, name, temporaryHomeExpiredTime(), source)
				: WaypointCrudService.add(player, name, source),
				temporary ? "commands.teleport_commands.home.tempSet" : "commands.teleport_commands.home.set",
				temporary ? "Error while setting a temporary home." : "Error while setting a home.");
	}

	static int updateHome(ServerPlayer player, String name) {
		return executeMutation(player, source -> WaypointCrudService.update(player, name, source),
				"commands.teleport_commands.home.update", "Error while updating a home.");
	}

	static int updateHomeFromManage(ServerPlayer player, UUID waypointUuid, WaypointListQuery query) {
		return executeMutation(player, source -> WaypointCrudService.update(player, waypointUuid, source),
				"commands.teleport_commands.home.update", "Error while updating a home.",
				currentPlayer -> HomeListHandler.renderHomeManage(currentPlayer, waypointUuid, query, false));
	}

	static int deleteHome(ServerPlayer player, String name) {
		return executeMutation(player, source -> WaypointCrudService.delete(name, source),
				"commands.teleport_commands.home.delete", "Error while deleting a home.");
	}

	static int deleteHomeFromManage(ServerPlayer player, UUID waypointUuid, WaypointListQuery query) {
		return executeMutation(player, source -> WaypointCrudService.delete(waypointUuid, source),
				"commands.teleport_commands.home.delete", "Error while deleting a home.",
				currentPlayer -> HomeListHandler.renderHomes(currentPlayer, query, false));
	}

	static int renameHome(ServerPlayer player, String oldName, String newName) {
		return executeMutation(player, source -> WaypointCrudService.rename(oldName, newName, source),
				"commands.teleport_commands.home.rename", "Error while renaming a home.");
	}

	static int setDefaultHome(ServerPlayer player, String name) {
		return executeMutation(player, source -> WaypointCrudService.setDefault(name, source),
				"commands.teleport_commands.home.default", "Error while setting default home.");
	}

	static int setDefaultHomeFromManage(ServerPlayer player, UUID waypointUuid, WaypointListQuery query) {
		return executeMutation(player, source -> WaypointCrudService.setDefault(waypointUuid, source),
				"commands.teleport_commands.home.default", "Error while setting default home.",
				currentPlayer -> HomeListHandler.renderHomeManage(currentPlayer, waypointUuid, query, false));
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
		AsyncWaypointSource source = source(player);
		if (source == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}

		int maxHomes = ConfigManager.query(config -> config.getHome().getPlayerMaximum());
		MinecraftServer server = player.level().getServer();
		UUID playerUuid = player.getUUID();
		WaypointMutationSupport.handle(player, mutation.apply(source), logMessage, () -> {
			WaypointMapSyncEvents.markPlayerDirty(playerUuid);
			SharedHomePublicationHandler.onOwnerHomesChanged(server, playerUuid);
		}, (currentPlayer, result, throwable) -> {
			if (throwable != null) {
				HomeMessages.send(currentPlayer, "commands.teleport_commands.common.error", ChatFormatting.RED,
						ChatFormatting.BOLD);
				return;
			}
			HomeMessages.sendMutationResult(currentPlayer, result, successKey, maxHomes);
			if (result == WaypointOperationResult.SUCCESS && successAction != null) {
				successAction.accept(currentPlayer);
			}
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

	private static AsyncWaypointSource source(ServerPlayer player) {
		PlayerProfileManager manager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (manager == null) {
			return null;
		}
		return new PlayerHomeSource(player.getUUID(), manager,
				() -> ConfigManager.query(config -> config.getHome().getPlayerMaximum()));
	}

	private static long temporaryHomeExpiredTime() {
		int ttlSeconds = ConfigManager.query(config -> config.getHome().getTemporaryHomeTtlSeconds());
		return System.currentTimeMillis() + TimeUtils.secondsToMillis(ttlSeconds);
	}
}
