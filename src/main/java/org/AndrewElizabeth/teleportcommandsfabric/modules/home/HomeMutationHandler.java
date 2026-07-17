package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.AsyncWaypointSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.PlayerHomeSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointCrudService;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointMapSyncEvents;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointOperationResult;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandAsyncSupport;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandReturns;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TimeUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

final class HomeMutationHandler {
	private HomeMutationHandler() {
	}

	static int setHome(ServerPlayer player, String name, boolean temporary) {
		if (!ensureEnabled(player)) {
			return CommandReturns.FAILED;
		}
		AsyncWaypointSource source = source(player);
		if (source == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}

		CompletableFuture<WaypointOperationResult> result = temporary
				? WaypointCrudService.addTemporary(player, name, temporaryHomeExpiredTime(), source)
				: WaypointCrudService.add(player, name, source);
		handleMutationResult(player, result, temporary ? "commands.teleport_commands.home.tempSet" : "commands.teleport_commands.home.set",
				temporary ? "Error while setting a temporary home." : "Error while setting a home.");
		return CommandReturns.ACCEPTED_ASYNC;
	}

	static int updateHome(ServerPlayer player, String name) {
		if (!ensureEnabled(player)) {
			return CommandReturns.FAILED;
		}
		AsyncWaypointSource source = source(player);
		if (source == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}
		handleMutationResult(player, WaypointCrudService.update(player, name, source),
				"commands.teleport_commands.home.update", "Error while updating a home.");
		return CommandReturns.ACCEPTED_ASYNC;
	}

	static int updateHomeFromManage(ServerPlayer player, UUID waypointUuid, WaypointListQuery query) {
		if (!ensureEnabled(player)) {
			return CommandReturns.FAILED;
		}
		AsyncWaypointSource source = source(player);
		if (source == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}
		handleMutationResult(player, WaypointCrudService.update(player, waypointUuid, source),
				"commands.teleport_commands.home.update", "Error while updating a home.",
				currentPlayer -> HomeListHandler.renderHomeManage(currentPlayer, waypointUuid, query, false));
		return CommandReturns.ACCEPTED_ASYNC;
	}

	static int deleteHome(ServerPlayer player, String name) {
		if (!ensureEnabled(player)) {
			return CommandReturns.FAILED;
		}
		AsyncWaypointSource source = source(player);
		if (source == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}
		handleMutationResult(player, WaypointCrudService.delete(name, source),
				"commands.teleport_commands.home.delete", "Error while deleting a home.");
		return CommandReturns.ACCEPTED_ASYNC;
	}

	static int deleteHomeFromManage(ServerPlayer player, UUID waypointUuid, WaypointListQuery query) {
		if (!ensureEnabled(player)) {
			return CommandReturns.FAILED;
		}
		AsyncWaypointSource source = source(player);
		if (source == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}
		handleMutationResult(player, WaypointCrudService.delete(waypointUuid, source),
				"commands.teleport_commands.home.delete", "Error while deleting a home.",
				currentPlayer -> HomeListHandler.renderHomes(currentPlayer, query, false));
		return CommandReturns.ACCEPTED_ASYNC;
	}

	static int renameHome(ServerPlayer player, String oldName, String newName) {
		if (!ensureEnabled(player)) {
			return CommandReturns.FAILED;
		}
		AsyncWaypointSource source = source(player);
		if (source == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}
		handleMutationResult(player, WaypointCrudService.rename(oldName, newName, source),
				"commands.teleport_commands.home.rename", "Error while renaming a home.");
		return CommandReturns.ACCEPTED_ASYNC;
	}

	static int setDefaultHome(ServerPlayer player, String name) {
		if (!ensureEnabled(player)) {
			return CommandReturns.FAILED;
		}
		AsyncWaypointSource source = source(player);
		if (source == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}
		handleMutationResult(player, WaypointCrudService.setDefault(name, source),
				"commands.teleport_commands.home.default", "Error while setting default home.");
		return CommandReturns.ACCEPTED_ASYNC;
	}

	static int setDefaultHomeFromManage(ServerPlayer player, UUID waypointUuid, WaypointListQuery query) {
		if (!ensureEnabled(player)) {
			return CommandReturns.FAILED;
		}
		AsyncWaypointSource source = source(player);
		if (source == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}
		handleMutationResult(player, WaypointCrudService.setDefault(waypointUuid, source),
				"commands.teleport_commands.home.default", "Error while setting default home.",
				currentPlayer -> HomeListHandler.renderHomeManage(currentPlayer, waypointUuid, query, false));
		return CommandReturns.ACCEPTED_ASYNC;
	}

	static int setMapVisibility(CommandContext<CommandSourceStack> context, boolean silent, WaypointListQuery query) {
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
		CommandAsyncSupport.whenCompleteForPlayer(server, playerUuid,
				WaypointCrudService.updateVisibility(name, visible, source), (currentPlayer, result, throwable) -> {
			if (throwable != null) {
				ModConstants.LOGGER.error("Error while updating home map visibility.", throwable);
				if (!silent) {
					HomeMessages.send(currentPlayer, "commands.teleport_commands.homes.error", ChatFormatting.RED, ChatFormatting.BOLD);
				}
				return;
			}
			if (result == WaypointOperationResult.SUCCESS) {
				WaypointMapSyncEvents.markPlayerDirty(playerUuid);
				SharedHomePublicationHandler.onOwnerHomesChanged(server, playerUuid);
			}
			if (!silent) {
				sendVisibilityResult(currentPlayer, result, visible);
				return;
			}
			if (query != null) {
				HomeListHandler.renderHomes(currentPlayer, query, false);
			}
		});
		return CommandReturns.ACCEPTED_ASYNC;
	}

	private static void handleMutationResult(ServerPlayer player, CompletableFuture<WaypointOperationResult> future,
			String successKey, String logMessage) {
		handleMutationResult(player, future, successKey, logMessage, null);
	}

	private static void handleMutationResult(ServerPlayer player, CompletableFuture<WaypointOperationResult> future,
			String successKey, String logMessage, Consumer<ServerPlayer> successAction) {
		int maxHomes = ConfigManager.query(config -> config.getHome().getPlayerMaximum());
		MinecraftServer server = player.level().getServer();
		UUID playerUuid = player.getUUID();
		future.whenComplete((result, throwable) -> {
			if (throwable == null && result == WaypointOperationResult.SUCCESS) {
				server.execute(() -> {
					WaypointMapSyncEvents.markPlayerDirty(playerUuid);
					SharedHomePublicationHandler.onOwnerHomesChanged(server, playerUuid);
				});
			}
		});
		CommandAsyncSupport.whenCompleteForPlayer(server, playerUuid, future, (currentPlayer, result, throwable) -> {
			if (throwable != null) {
				ModConstants.LOGGER.error(logMessage, throwable);
				HomeMessages.send(currentPlayer, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
				return;
			}
			sendMutationResult(currentPlayer, result, successKey, maxHomes);
			if (result == WaypointOperationResult.SUCCESS && successAction != null) {
				successAction.accept(currentPlayer);
			}
		});
	}

	private static void sendMutationResult(ServerPlayer player, WaypointOperationResult result, String successKey, int maxHomes) {
		switch (result) {
		case SUCCESS -> HomeMessages.send(player, successKey, ChatFormatting.GREEN);
		case SAME_LOCATION -> HomeMessages.send(player, "commands.teleport_commands.home.updateSame", ChatFormatting.AQUA);
		case SAME_DEFAULT -> HomeMessages.send(player, "commands.teleport_commands.home.defaultSame", ChatFormatting.AQUA);
		case NOT_FOUND -> HomeMessages.send(player, "commands.teleport_commands.home.notFound", ChatFormatting.RED);
		case ALREADY_EXISTS -> HomeMessages.send(player, "commands.teleport_commands.home.exists", ChatFormatting.RED);
		case LIMIT_REACHED -> HomeMessages.sendMaxReached(player, maxHomes);
		case TEMP_HOME_EXISTS -> HomeMessages.send(player, "commands.teleport_commands.home.tempExists", ChatFormatting.RED);
		case CANNOT_BE_DEFAULT -> HomeMessages.send(player, "commands.teleport_commands.home.defaultTemporary", ChatFormatting.RED);
		case DEFAULT_NOT_SUPPORTED -> HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED,
				ChatFormatting.BOLD);
		default -> HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	private static void sendVisibilityResult(ServerPlayer player, WaypointOperationResult result, boolean visible) {
		switch (result) {
		case SUCCESS -> HomeMessages.send(player, visible
				? "commands.teleport_commands.home.mapShown"
				: "commands.teleport_commands.home.mapHidden", ChatFormatting.GREEN);
		case NOT_FOUND -> HomeMessages.send(player, "commands.teleport_commands.home.notFound", ChatFormatting.RED);
		default -> HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	private static boolean ensureEnabled(ServerPlayer player) {
		return ensureEnabled(player, false);
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
		return new PlayerHomeSource(player.getUUID(), manager, () -> ConfigManager.query(config -> config.getHome().getPlayerMaximum()));
	}

	private static long temporaryHomeExpiredTime() {
		int ttlSeconds = ConfigManager.query(config -> config.getHome().getTemporaryHomeTtlSeconds());
		return System.currentTimeMillis() + TimeUtils.secondsToMillis(ttlSeconds);
	}
}
