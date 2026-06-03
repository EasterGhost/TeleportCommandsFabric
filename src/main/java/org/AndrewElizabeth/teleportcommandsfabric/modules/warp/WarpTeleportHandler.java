package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.Config;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.TargetTeleportOptions;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.TeleportRequest;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.AsyncWaypointSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.GlobalWarpSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointCrudService;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class WarpTeleportHandler {
	private static final int TICKS_PER_SECOND = 20;
	private static final long MILLIS_PER_SECOND = 1000L;

	private WarpTeleportHandler() {
	}

	static int teleportWarp(ServerPlayer player, String name, boolean safetyDisabled) {
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
		resolveWarp(name).whenComplete((location, throwable) -> server.execute(() -> {
			ServerPlayer currentPlayer = server.getPlayerList().getPlayer(playerUuid);
			if (currentPlayer == null) {
				return;
			}
			if (throwable != null) {
				ModConstants.LOGGER.error("Error while resolving warp.", throwable);
				WarpMessages.send(currentPlayer, "commands.teleport_commands.warp.goError", ChatFormatting.RED, ChatFormatting.BOLD);
				return;
			}
			if (location == null || location.isEmpty()) {
				WarpMessages.send(currentPlayer, "commands.teleport_commands.warp.notFound", ChatFormatting.RED);
				return;
			}
			executeTeleport(currentPlayer, location.get(), settings, safetyDisabled);
		}));
		return 0;
	}

	private static void executeTeleport(ServerPlayer player, NamedLocationView warp, WarpCommandSettings settings,
			boolean safetyDisabled) {
		MinecraftServer server = player.level().getServer();
		ServerLevel world = server.getLevel(warp.getDimension());
		if (world == null) {
			ModConstants.LOGGER.warn("Cannot execute /warp for {}: world {} was not found.",
					player.getName().getString(), warp.getDimensionId());
			WarpMessages.send(player, "commands.teleport_commands.common.worldNotFound", ChatFormatting.RED, ChatFormatting.BOLD);
			if (settings.deleteInvalidWarps()) {
				AsyncWaypointSource source = source();
				if (source != null) {
					WaypointCrudService.delete(warp.getName(), source).whenComplete((ignored, throwable) -> server.execute(() -> {
						if (throwable == null) {
							ServerPlayer currentPlayer = server.getPlayerList().getPlayer(player.getUUID());
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

		TargetTeleportOptions options = TargetTeleportOptions.builder()
				.delayTicks(settings.delayTicks())
				.cooldownMillis(settings.cooldownMillis())
				.safetyEnabled(!safetyDisabled)
				.recordPrevious(true)
				.build();
		TeleportTarget target = TeleportTarget.of(world, new Vec3(
				warp.getX() + 0.5D,
				warp.getYPrecise(),
				warp.getZ() + 0.5D));
		TeleportRequest request = TeleportRequest.resolved(target, options);
		String forceCommand = "warp " + CommandArgumentUtils.quote(warp.getName()) + " true";
		try {
			TeleportService service = TeleportCommands.TELEPORT_SERVICE;
			CompletableFuture<TeleportStatus> result = service.request(player, request);
			if (result.isDone()) {
				WarpMessages.sendStatus(player, result.join(), settings.cooldownSeconds(), forceCommand);
				return;
			}
			if (settings.delaySeconds() > 0) {
				WarpMessages.sendDelayStart(player, settings.delaySeconds());
			} else {
				WarpMessages.send(player, "commands.teleport_commands.warp.go", ChatFormatting.AQUA);
			}
			result.whenComplete((status, throwable) -> server.execute(() -> {
				ServerPlayer currentPlayer = server.getPlayerList().getPlayer(player.getUUID());
				if (currentPlayer == null) {
					return;
				}
				if (throwable != null) {
					ModConstants.LOGGER.error("Error while executing /warp teleport.", throwable);
					WarpMessages.send(currentPlayer, "commands.teleport_commands.warp.goError", ChatFormatting.RED,
							ChatFormatting.BOLD);
					return;
				}
				WarpMessages.sendStatus(currentPlayer, status, settings.cooldownSeconds(), forceCommand);
			}));
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Error while executing /warp teleport.", exception);
			WarpMessages.send(player, "commands.teleport_commands.warp.goError", ChatFormatting.RED, ChatFormatting.BOLD);
		}
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
		return new WarpCommandSettings(delaySeconds, delaySeconds * TICKS_PER_SECOND,
				cooldownSeconds, cooldownSeconds * MILLIS_PER_SECOND,
				config.getWarp().isDeleteInvalid());
	}

	private record WarpCommandSettings(int delaySeconds, int delayTicks, int cooldownSeconds, long cooldownMillis,
			boolean deleteInvalidWarps) {
	}
}
