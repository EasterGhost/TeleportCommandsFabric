package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.Config;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointTeleportTargets;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeKey;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeResolver;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeView;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandAsyncSupport;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandReturns;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.TargetTeleportCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.TargetTeleportSafety;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TimeUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class SharedHomeTeleportHandler {
	private SharedHomeTeleportHandler() {
	}

	static int teleportByName(ServerPlayer player, String name, Boolean safetyDisabledOverride) {
		if (!ensureAvailable(player)) {
			return CommandReturns.FAILED;
		}
		MinecraftServer server = player.level().getServer();
		UUID playerUuid = player.getUUID();
		CommandAsyncSupport.whenCompleteForPlayer(server, playerUuid,
				SharedHomeResolver.resolveSubscriptions(playerUuid, TeleportCommands.SHARED_HOME_SERVICE,
						TeleportCommands.PLAYER_PROFILE_MANAGER, server),
				(currentPlayer, homes, throwable) -> {
			if (throwable != null) {
				ModConstants.LOGGER.error("Failed to resolve a shared home by name.", throwable);
				SharedHomeMessages.send(currentPlayer, "commands.teleport_commands.sharedhome.goError",
						ChatFormatting.RED, ChatFormatting.BOLD);
				return;
			}
			List<SharedHomeView> matches = homes.stream()
					.filter(home -> home.getName().toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT)))
					.toList();
			if (matches.isEmpty()) {
				SharedHomeMessages.send(currentPlayer, "commands.teleport_commands.sharedhome.notFound", ChatFormatting.AQUA);
				return;
			}
			if (matches.size() > 1) {
				SharedHomeMessages.send(currentPlayer, "commands.teleport_commands.sharedhome.ambiguous", ChatFormatting.RED);
				return;
			}
			executeTeleport(currentPlayer, matches.getFirst().key(), matches.getFirst(), settings(), safetyDisabledOverride);
		});
		return CommandReturns.ACCEPTED_ASYNC;
	}

	static int teleportByKey(ServerPlayer player, SharedHomeKey key, Boolean safetyDisabledOverride) {
		if (!ensureAvailable(player)) {
			return CommandReturns.FAILED;
		}
		if (!TeleportCommands.SHARED_HOME_SERVICE.isSubscribed(player.getUUID(), key)) {
			SharedHomeMessages.send(player, "commands.teleport_commands.sharedhome.notSubscribed", ChatFormatting.RED);
			return CommandReturns.FAILED;
		}
		MinecraftServer server = player.level().getServer();
		UUID playerUuid = player.getUUID();
		CommandAsyncSupport.whenCompleteForPlayer(server, playerUuid,
				SharedHomeResolver.resolve(key, TeleportCommands.PLAYER_PROFILE_MANAGER),
				(currentPlayer, home, throwable) -> {
			if (throwable != null) {
				ModConstants.LOGGER.error("Failed to resolve a shared home by UUID.", throwable);
				SharedHomeMessages.send(currentPlayer, "commands.teleport_commands.sharedhome.goError",
						ChatFormatting.RED, ChatFormatting.BOLD);
				return;
			}
			if (home == null || home.isEmpty() || home.get().isTemporary()) {
				SharedHomePublicationHandler.invalidateMissing(key);
				SharedHomeMessages.send(currentPlayer, "commands.teleport_commands.sharedhome.unavailable", ChatFormatting.RED);
				return;
			}
			executeTeleport(currentPlayer, key, home.get(), settings(), safetyDisabledOverride);
		});
		return CommandReturns.ACCEPTED_ASYNC;
	}

	private static void executeTeleport(ServerPlayer player, SharedHomeKey key, NamedLocationView home,
			Settings settings, Boolean safetyDisabledOverride) {
		if (!TeleportCommands.SHARED_HOME_SERVICE.isSubscribed(player.getUUID(), key)) {
			SharedHomeMessages.send(player, "commands.teleport_commands.sharedhome.notSubscribed", ChatFormatting.RED);
			return;
		}
		MinecraftServer server = player.level().getServer();
		ServerLevel world = server.getLevel(home.getDimension());
		if (world == null) {
			SharedHomeMessages.send(player, "commands.teleport_commands.common.worldNotFound",
					ChatFormatting.RED, ChatFormatting.BOLD);
			return;
		}
		if (player.level().dimension().equals(home.getDimension()) && player.blockPosition().equals(home.getBlockPos())) {
			SharedHomeMessages.send(player, "commands.teleport_commands.sharedhome.goSame", ChatFormatting.AQUA);
			return;
		}

		TeleportTarget target = WaypointTeleportTargets.toTarget(home, world);
		String forceCommand = "teleportcommandsfabric:sharedhome " + key.ownerUuid() + " " + key.homeUuid() + " true";
		TargetTeleportCommandSupport.submit(player, target, new TargetTeleportCommandSupport.Settings(
				settings.delaySeconds(), settings.delayTicks(), settings.cooldownSeconds(), settings.cooldownMillis(),
				TargetTeleportSafety.resolveEnabled(settings.defaultSafetyCheck(), safetyDisabledOverride), true),
				"commands.teleport_commands.sharedhome.go", "commands.teleport_commands.sharedhome.goError",
				"Error while executing /sharedhome teleport.", forceCommand, HomeMessages::sendStatus);
	}

	private static Settings settings() {
		return ConfigManager.query(SharedHomeTeleportHandler::settingsFrom);
	}

	private static Settings settingsFrom(Config config) {
		int delaySeconds = config.getTeleporting().getDelay();
		int cooldownSeconds = config.getTeleporting().getCooldown();
		return new Settings(delaySeconds, TimeUtils.secondsToTicks(delaySeconds), cooldownSeconds,
				TimeUtils.secondsToMillis(cooldownSeconds), config.getTeleporting().isDefaultSafetyCheck());
	}

	private static boolean ensureAvailable(ServerPlayer player) {
		if (!ConfigManager.query(config -> config.getHome().isEnabled())) {
			HomeMessages.send(player, "commands.teleport_commands.home.disabled", ChatFormatting.RED);
			return false;
		}
		if (TeleportCommands.SHARED_HOME_SERVICE == null || TeleportCommands.PLAYER_PROFILE_MANAGER == null
				|| TeleportCommands.TELEPORT_SERVICE == null) {
			SharedHomeMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return false;
		}
		return true;
	}

	private record Settings(int delaySeconds, int delayTicks, int cooldownSeconds, long cooldownMillis,
			boolean defaultSafetyCheck) {
	}
}
