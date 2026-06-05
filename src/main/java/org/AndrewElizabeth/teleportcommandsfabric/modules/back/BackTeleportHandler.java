package org.AndrewElizabeth.teleportcommandsfabric.modules.back;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.Config;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.record.AsyncRecordedLocationSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.TargetTeleportOptions;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.TeleportRequest;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.TargetTeleportSafety;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TimeUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class BackTeleportHandler {
	private static final String COMMAND_BACK = "back";
	private static final String MODE_DEATH = "death";
	private static final String MODE_TP = "tp";
	private static final String COMMAND_BACK_DEATH_FORCE = COMMAND_BACK + " " + MODE_DEATH + " true";
	private static final String COMMAND_BACK_TP_FORCE = COMMAND_BACK + " " + MODE_TP + " true";

	private BackTeleportHandler() {
	}

	static int handleBackDeath(ServerPlayer player, Boolean safetyDisabledOverride) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		AsyncRecordedLocationSource source = TeleportCommands.RECORDED_LOCATION_SOURCE;
		if (source == null) {
			BackMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}

		UUID playerUuid = player.getUUID();
		MinecraftServer server = player.level().getServer();
		source.getDeathLocation(playerUuid).whenComplete((location, throwable) -> server.execute(() -> {
			ServerPlayer currentPlayer = server.getPlayerList().getPlayer(playerUuid);
			if (currentPlayer == null) {
				return;
			}
			if (throwable != null) {
				ModConstants.LOGGER.error("Error while going back.", throwable);
				BackMessages.send(currentPlayer, "commands.teleport_commands.common.error", ChatFormatting.RED,
						ChatFormatting.BOLD);
				return;
			}
			if (location == null || location.isEmpty()) {
				BackMessages.send(currentPlayer, "commands.teleport_commands.common.noLocation", ChatFormatting.RED);
				promptPreviousIfPresent(source, playerUuid, currentPlayer);
				return;
			}
			executeResolved(currentPlayer, location.get(), safetyDisabledOverride, COMMAND_BACK_DEATH_FORCE, true,
					"commands.teleport_commands.back.same", "commands.teleport_commands.back.go");
		}));
		return 0;
	}

	static int handleBackTp(ServerPlayer player, Boolean safetyDisabledOverride) {
		if (!ensureEnabled(player)) {
			return 1;
		}
		AsyncRecordedLocationSource source = TeleportCommands.RECORDED_LOCATION_SOURCE;
		if (source == null) {
			BackMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}

		UUID playerUuid = player.getUUID();
		MinecraftServer server = player.level().getServer();
		source.getPreviousTeleportLocation(playerUuid)
				.whenComplete((location, throwable) -> server.execute(() -> {
					ServerPlayer currentPlayer = server.getPlayerList().getPlayer(playerUuid);
					if (currentPlayer == null) {
						return;
					}
					if (throwable != null) {
						ModConstants.LOGGER.error("Error while going back to previous teleport location.", throwable);
						BackMessages.send(currentPlayer, "commands.teleport_commands.common.error", ChatFormatting.RED,
								ChatFormatting.BOLD);
						return;
					}
					if (location == null || location.isEmpty()) {
						BackMessages.send(currentPlayer, "commands.teleport_commands.back.tp.none", ChatFormatting.RED);
						return;
					}
					executeResolved(currentPlayer, location.get(), safetyDisabledOverride, COMMAND_BACK_TP_FORCE, false,
							"commands.teleport_commands.back.tp.same", "commands.teleport_commands.back.tp.go");
				}));
		return 0;
	}

	private static void executeResolved(ServerPlayer player, RecordedLocationView location, Boolean safetyDisabledOverride,
			String forceCommand, boolean removeDeathOnSuccess, String sameKey, String goKey) {
		MinecraftServer server = player.level().getServer();
		ServerLevel world = server.getLevel(location.getDimension());
		if (world == null) {
			ModConstants.LOGGER.warn("Cannot execute /back for {}: world {} was not found.",
					player.getName().getString(), location.getDimensionId());
			BackMessages.send(player, "commands.teleport_commands.common.worldNotFound", ChatFormatting.RED, ChatFormatting.BOLD);
			return;
		}
		if (player.level().dimension().equals(location.getDimension()) && player.blockPosition().equals(location.getBlockPos())) {
			BackMessages.send(player, sameKey, ChatFormatting.AQUA);
			return;
		}

		BackCommandSettings settings = ConfigManager.query(BackTeleportHandler::settingsFrom);
		TeleportService service = TeleportCommands.TELEPORT_SERVICE;
		if (service == null) {
			BackMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return;
		}

		TargetTeleportOptions options = TargetTeleportOptions.builder()
				.delayTicks(settings.delayTicks())
				.cooldownMillis(settings.cooldownMillis())
				.safetyEnabled(settings.safetyEnabled(safetyDisabledOverride))
				.recordPrevious(removeDeathOnSuccess)
				.build();
		TeleportRequest request = TeleportRequest.resolved(TeleportTarget.centered(world, location.getBlockPos()), options);

		try {
			CompletableFuture<TeleportStatus> result = service.request(player, request);
			if (result.isDone()) {
				BackMessages.sendStatus(player, result.join(), settings.cooldownSeconds(), forceCommand);
				return;
			}
			if (settings.delaySeconds() > 0) {
				BackMessages.sendDelayStart(player, settings.delaySeconds());
			} else {
				BackMessages.send(player, goKey, ChatFormatting.AQUA);
			}
			result.whenComplete((status, throwable) -> server.execute(() -> {
				ServerPlayer currentPlayer = server.getPlayerList().getPlayer(player.getUUID());
				if (currentPlayer == null) {
					return;
				}
				if (throwable != null) {
					ModConstants.LOGGER.error("Error while executing /back teleport.", throwable);
					BackMessages.send(currentPlayer, "commands.teleport_commands.common.error", ChatFormatting.RED,
							ChatFormatting.BOLD);
					return;
				}
				if (status == TeleportStatus.SUCCESS && removeDeathOnSuccess && settings.deleteDeathAfterTeleport()
						&& TeleportCommands.RECORDED_LOCATION_SOURCE != null) {
					TeleportCommands.RECORDED_LOCATION_SOURCE.removeDeathLocation(currentPlayer.getUUID());
				}
				BackMessages.sendStatus(currentPlayer, status, settings.cooldownSeconds(), forceCommand);
			}));
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Error while executing /back teleport.", exception);
			BackMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	private static void promptPreviousIfPresent(AsyncRecordedLocationSource source, UUID playerUuid, ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		source.getPreviousTeleportLocation(playerUuid).whenComplete((previous, throwable) -> server.execute(() -> {
			ServerPlayer currentPlayer = server.getPlayerList().getPlayer(playerUuid);
			if (currentPlayer != null && throwable == null && previous != null && previous.isPresent()) {
				BackMessages.sendTryBackTpPrompt(currentPlayer);
			}
		}));
	}

	private static boolean ensureEnabled(ServerPlayer player) {
		if (ConfigManager.query(config -> config.getBack().isEnabled())) {
			return true;
		}
		BackMessages.send(player, "commands.teleport_commands.back.disabled", ChatFormatting.RED);
		return false;
	}

	private static BackCommandSettings settingsFrom(Config config) {
		int delaySeconds = config.getTeleporting().getDelay();
		int cooldownSeconds = config.getTeleporting().getCooldown();
		return new BackCommandSettings(delaySeconds, TimeUtils.secondsToTicks(delaySeconds), cooldownSeconds,
				TimeUtils.secondsToMillis(cooldownSeconds), config.getBack().isDeleteAfterTeleport(),
				config.getTeleporting().isDefaultSafetyCheck());
	}

	private record BackCommandSettings(int delaySeconds, int delayTicks, int cooldownSeconds, long cooldownMillis,
			boolean deleteDeathAfterTeleport, boolean defaultSafetyCheck) {
		private boolean safetyEnabled(Boolean safetyDisabledOverride) {
			return TargetTeleportSafety.resolveEnabled(defaultSafetyCheck, safetyDisabledOverride);
		}
	}
}
