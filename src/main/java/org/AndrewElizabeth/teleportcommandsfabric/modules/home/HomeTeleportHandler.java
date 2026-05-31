package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

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
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.PlayerHomeSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointCrudService;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

final class HomeTeleportHandler {
	private static final int TICKS_PER_SECOND = 20;
	private static final long MILLIS_PER_SECOND = 1000L;

	private HomeTeleportHandler() {
	}

	static int teleportHome(ServerPlayer player, String name, boolean safetyDisabled) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		HomeCommandSettings settings = ConfigManager.query(HomeTeleportHandler::settingsFrom);
		AsyncWaypointSource source = source(player);
		if (source == null || TeleportCommands.TELEPORT_SERVICE == null) {
			HomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		resolveHome(player, name).whenComplete((location, throwable) -> player.level().getServer().execute(() -> {
			ServerPlayer currentPlayer = player.level().getServer().getPlayerList().getPlayer(player.getUUID());
			if (currentPlayer == null) {
				return;
			}
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
			executeTeleport(currentPlayer, location.get(), source, settings, safetyDisabled);
		}));
		return 0;
	}

	private static void executeTeleport(ServerPlayer player, NamedLocationView home, AsyncWaypointSource source,
			HomeCommandSettings settings, boolean safetyDisabled) {
		MinecraftServer server = player.level().getServer();
		ServerLevel world = server.getLevel(home.getDimension());
		if (world == null) {
			ModConstants.LOGGER.warn("Cannot execute /home for {}: world {} was not found.",
					player.getName().getString(), home.getDimensionId());
			HomeMessages.send(player, "commands.teleport_commands.common.worldNotFound", ChatFormatting.RED, ChatFormatting.BOLD);
			if (settings.deleteInvalidHomes()) {
				WaypointCrudService.delete(home.getName(), source).whenComplete((ignored, throwable) -> server.execute(() -> {
					if (throwable == null) {
						HomeMessages.send(player, "commands.teleport_commands.home.deletedInvalid", ChatFormatting.YELLOW);
					}
				}));
			}
			return;
		}
		if (player.level().dimension().equals(home.getDimension()) && player.blockPosition().equals(home.getBlockPos())) {
			HomeMessages.send(player, "commands.teleport_commands.home.goSame", ChatFormatting.AQUA);
			return;
		}

		TargetTeleportOptions options = TargetTeleportOptions.builder()
				.delayTicks(settings.delayTicks())
				.cooldownMillis(settings.cooldownMillis())
				.safetyEnabled(!safetyDisabled)
				.recordPrevious(true)
				.build();
		TeleportTarget target = TeleportTarget.of(world, new Vec3(
				home.getX() + 0.5D,
				home.getYPrecise(),
				home.getZ() + 0.5D));
		TeleportRequest request = TeleportRequest.resolved(target, options);
		String forceCommand = "home " + CommandArgumentUtils.quote(home.getName()) + " true";
		try {
			TeleportService service = TeleportCommands.TELEPORT_SERVICE;
			CompletableFuture<TeleportStatus> result = service.request(player, request);
			if (result.isDone()) {
				HomeMessages.sendStatus(player, result.join(), settings.cooldownSeconds(), forceCommand);
				return;
			}
			if (settings.delaySeconds() > 0) {
				HomeMessages.sendDelayStart(player, settings.delaySeconds());
			} else {
				HomeMessages.send(player, "commands.teleport_commands.home.go", ChatFormatting.AQUA);
			}
			result.whenComplete((status, throwable) -> server.execute(() -> {
				ServerPlayer currentPlayer = server.getPlayerList().getPlayer(player.getUUID());
				if (currentPlayer == null) {
					return;
				}
				if (throwable != null) {
					ModConstants.LOGGER.error("Error while executing /home teleport.", throwable);
					HomeMessages.send(currentPlayer, "commands.teleport_commands.home.goError", ChatFormatting.RED,
							ChatFormatting.BOLD);
					return;
				}
				HomeMessages.sendStatus(currentPlayer, status, settings.cooldownSeconds(), forceCommand);
			}));
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Error while executing /home teleport.", exception);
			HomeMessages.send(player, "commands.teleport_commands.home.goError", ChatFormatting.RED, ChatFormatting.BOLD);
		}
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
		return new HomeCommandSettings(delaySeconds, delaySeconds * TICKS_PER_SECOND,
				cooldownSeconds, cooldownSeconds * MILLIS_PER_SECOND,
				config.getHome().isDeleteInvalid());
	}

	private record HomeCommandSettings(int delaySeconds, int delayTicks, int cooldownSeconds, long cooldownMillis,
			boolean deleteInvalidHomes) {
	}
}
