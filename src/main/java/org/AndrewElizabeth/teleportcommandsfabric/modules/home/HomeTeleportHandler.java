package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.Config;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.AsyncWaypointSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.PlayerHomeSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointMapSyncEvents;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointCrudService;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointOperationResult;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointTeleportTargets;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandAsyncSupport;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandReturns;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.TargetTeleportSafety;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.TargetTeleportCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TimeUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class HomeTeleportHandler {
	private HomeTeleportHandler() {
	}

	static int teleportHome(ServerPlayer player, String name, Boolean safetyDisabledOverride) {
		if (!ensureEnabled(player)) {
			return CommandReturns.FAILED;
		}
		HomeCommandSettings settings = ConfigManager.query(HomeTeleportHandler::settingsFrom);
		AsyncWaypointSource source = source(player);
		if (source == null || TeleportCommands.TELEPORT_SERVICE == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}
		MinecraftServer server = player.level().getServer();
		UUID playerUuid = player.getUUID();
		CommandAsyncSupport.whenCompleteForPlayer(server, playerUuid, resolveHome(player, name),
				(currentPlayer, location, throwable) -> {
			if (throwable != null) {
				ModConstants.LOGGER.error("Error while resolving home.", throwable);
				HomeMessages.send(currentPlayer, "commands.teleport_commands.home.goError", ChatFormatting.RED, ChatFormatting.BOLD);
				return;
			}
			if (location == null || location.isEmpty()) {
				HomeMessages.send(currentPlayer,
						name == null || name.isBlank() ? "commands.teleport_commands.home.defaultNone" : "commands.teleport_commands.home.notFound",
						ChatFormatting.AQUA);
				return;
			}
			executeTeleport(currentPlayer, location.get(), source, settings, safetyDisabledOverride);
		});
		return CommandReturns.ACCEPTED_ASYNC;
	}

	private static void executeTeleport(ServerPlayer player, NamedLocationView home, AsyncWaypointSource source,
			HomeCommandSettings settings, Boolean safetyDisabledOverride) {
		MinecraftServer server = player.level().getServer();
		ServerLevel world = server.getLevel(home.getDimension());
		if (world == null) {
			ModConstants.LOGGER.warn("Cannot execute /home for {}: world {} was not found.",
					player.getName().getString(), home.getDimensionId());
			HomeMessages.send(player, "commands.teleport_commands.common.worldNotFound", ChatFormatting.RED, ChatFormatting.BOLD);
			if (settings.deleteInvalidHomes()) {
				UUID playerUuid = player.getUUID();
				WaypointCrudService.deleteIfUnchanged(home, source).whenComplete((result, throwable) -> server.execute(() -> {
					if (throwable == null && result == WaypointOperationResult.SUCCESS) {
						WaypointMapSyncEvents.markPlayerDirty(playerUuid);
					}
					ServerPlayer currentPlayer = server.getPlayerList().getPlayer(playerUuid);
					if (throwable == null && result == WaypointOperationResult.SUCCESS && currentPlayer != null) {
						HomeMessages.send(currentPlayer, "commands.teleport_commands.home.deletedInvalid", ChatFormatting.YELLOW);
					}
				}));
			}
			return;
		}
		if (player.level().dimension().equals(home.getDimension()) && player.blockPosition().equals(home.getBlockPos())) {
			HomeMessages.send(player, "commands.teleport_commands.home.goSame", ChatFormatting.AQUA);
			return;
		}

		TeleportTarget target = WaypointTeleportTargets.toTarget(home, world);
		String forceCommand = "home " + CommandArgumentUtils.quote(home.getName()) + " true";
		TargetTeleportCommandSupport.submit(player, target, new TargetTeleportCommandSupport.Settings(
				settings.delaySeconds(), settings.delayTicks(), settings.cooldownSeconds(), settings.cooldownMillis(),
				settings.safetyEnabled(safetyDisabledOverride), true),
				"commands.teleport_commands.home.go", "commands.teleport_commands.home.goError",
				"Error while executing /home teleport.", forceCommand, HomeMessages::sendStatus);
	}

	private static CompletableFuture<Optional<NamedLocationView>> resolveHome(ServerPlayer player, String name) {
		PlayerProfileManager manager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (manager == null) {
			return CompletableFuture.completedFuture(Optional.empty());
		}
		if (name != null && !name.isBlank()) {
			return manager.query(player.getUUID(), profile -> profile.getHomeByName(name).map(home -> (NamedLocationView) home));
		}
		return manager.query(player.getUUID(), profile -> profile.getDefaultHomeLocation().map(home -> (NamedLocationView) home));
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
		return new PlayerHomeSource(player.getUUID(), manager, () -> ConfigManager.query(config -> config.getHome().getPlayerMaximum()));
	}

	private static HomeCommandSettings settingsFrom(Config config) {
		int delaySeconds = config.getTeleporting().getDelay();
		int cooldownSeconds = config.getTeleporting().getCooldown();
		return new HomeCommandSettings(delaySeconds, TimeUtils.secondsToTicks(delaySeconds),
				cooldownSeconds, TimeUtils.secondsToMillis(cooldownSeconds),
				config.getHome().isDeleteInvalid(), config.getTeleporting().isDefaultSafetyCheck());
	}

	private record HomeCommandSettings(int delaySeconds, int delayTicks, int cooldownSeconds, long cooldownMillis,
			boolean deleteInvalidHomes, boolean defaultSafetyCheck) {
		private boolean safetyEnabled(Boolean safetyDisabledOverride) {
			return TargetTeleportSafety.resolveEnabled(defaultSafetyCheck, safetyDisabledOverride);
		}
	}
}
