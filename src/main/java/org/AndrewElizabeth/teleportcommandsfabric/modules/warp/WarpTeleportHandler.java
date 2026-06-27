package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.Config;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.AsyncWaypointSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.GlobalWarpSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointMapSyncEvents;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointCrudService;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointOperationResult;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointTeleportTargets;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandAsyncSupport;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.TargetTeleportCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.TargetTeleportSafety;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileManager;
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

final class WarpTeleportHandler {
	private WarpTeleportHandler() {
	}

	static int teleportWarp(ServerPlayer player, String name, Boolean safetyDisabledOverride) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		WarpCommandSettings settings = ConfigManager.query(WarpTeleportHandler::settingsFrom);
		if (TeleportCommands.GLOBAL_PROFILE_MANAGER == null || TeleportCommands.TELEPORT_SERVICE == null) {
			WarpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		MinecraftServer server = player.level().getServer();
		UUID playerUuid = player.getUUID();
		CommandAsyncSupport.whenCompleteForPlayer(server, playerUuid, resolveWarp(name),
				(currentPlayer, location, throwable) -> {
			if (throwable != null) {
				ModConstants.LOGGER.error("Error while resolving warp.", throwable);
				WarpMessages.send(currentPlayer, "commands.teleport_commands.warp.goError", ChatFormatting.RED, ChatFormatting.BOLD);
				return;
			}
			if (location == null || location.isEmpty()) {
				WarpMessages.send(currentPlayer, "commands.teleport_commands.warp.notFound", ChatFormatting.RED);
				return;
			}
			executeTeleport(currentPlayer, location.get(), settings, safetyDisabledOverride);
		});
		return 0;
	}

	private static void executeTeleport(ServerPlayer player, NamedLocationView warp, WarpCommandSettings settings,
			Boolean safetyDisabledOverride) {
		MinecraftServer server = player.level().getServer();
		ServerLevel world = server.getLevel(warp.getDimension());
		if (world == null) {
			ModConstants.LOGGER.warn("Cannot execute /warp for {}: world {} was not found.",
					player.getName().getString(), warp.getDimensionId());
			WarpMessages.send(player, "commands.teleport_commands.common.worldNotFound", ChatFormatting.RED, ChatFormatting.BOLD);
			if (settings.deleteInvalidWarps()) {
				AsyncWaypointSource source = source();
				if (source != null) {
					UUID playerUuid = player.getUUID();
					WaypointCrudService.delete(warp.getName(), source).whenComplete((result, throwable) -> server.execute(() -> {
						if (throwable == null && result == WaypointOperationResult.SUCCESS) {
							WaypointMapSyncEvents.markAllDirty();
							ServerPlayer currentPlayer = server.getPlayerList().getPlayer(playerUuid);
							if (currentPlayer == null) {
								return;
							}
							if (TeleportCommands.WAYPOINT_PAGES != null) {
								TeleportCommands.WAYPOINT_PAGES.invalidateWarpCache();
							}
							WarpMessages.send(currentPlayer, "commands.teleport_commands.warp.deletedInvalid", ChatFormatting.YELLOW);
						}
					}));
				}
			}
			return;
		}
		if (player.level().dimension().equals(warp.getDimension()) && player.blockPosition().equals(warp.getBlockPos())) {
			WarpMessages.send(player, "commands.teleport_commands.warp.goSame", ChatFormatting.AQUA);
			return;
		}

		TeleportTarget target = WaypointTeleportTargets.toTarget(warp, world);
		String forceCommand = "warp " + CommandArgumentUtils.quote(warp.getName()) + " true";
		TargetTeleportCommandSupport.submit(player, target, new TargetTeleportCommandSupport.Settings(
				settings.delaySeconds(), settings.delayTicks(), settings.cooldownSeconds(), settings.cooldownMillis(),
				settings.safetyEnabled(safetyDisabledOverride), true),
				"commands.teleport_commands.warp.go", "commands.teleport_commands.warp.goError",
				"Error while executing /warp teleport.", forceCommand, WarpMessages::sendStatus);
	}

	private static CompletableFuture<Optional<NamedLocationView>> resolveWarp(String name) {
		GlobalProfileManager manager = TeleportCommands.GLOBAL_PROFILE_MANAGER;
		if (manager == null) {
			return CompletableFuture.completedFuture(Optional.empty());
		}
		return manager.query(profile -> profile.getWarpByName(name));
	}

	private static boolean ensureEnabled(ServerPlayer player) {
		if (ConfigManager.query(config -> config.getWarp().isEnabled())) {
			return true;
		}
		WarpMessages.send(player, "commands.teleport_commands.warp.disabled", ChatFormatting.RED);
		return false;
	}

	private static AsyncWaypointSource source() {
		GlobalProfileManager manager = TeleportCommands.GLOBAL_PROFILE_MANAGER;
		if (manager == null) {
			return null;
		}
		return new GlobalWarpSource(manager, () -> ConfigManager.query(config -> config.getWarp().getMaximum()));
	}

	private static WarpCommandSettings settingsFrom(Config config) {
		int delaySeconds = config.getTeleporting().getDelay();
		int cooldownSeconds = config.getTeleporting().getCooldown();
		return new WarpCommandSettings(delaySeconds, TimeUtils.secondsToTicks(delaySeconds),
				cooldownSeconds, TimeUtils.secondsToMillis(cooldownSeconds),
				config.getWarp().isDeleteInvalid(), config.getTeleporting().isDefaultSafetyCheck());
	}

	private record WarpCommandSettings(int delaySeconds, int delayTicks, int cooldownSeconds, long cooldownMillis,
			boolean deleteInvalidWarps, boolean defaultSafetyCheck) {
		private boolean safetyEnabled(Boolean safetyDisabledOverride) {
			return TargetTeleportSafety.resolveEnabled(defaultSafetyCheck, safetyDisabledOverride);
		}
	}
}
