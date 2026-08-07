package org.AndrewElizabeth.teleportcommandsfabric.modules.wild;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.Config;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.WildService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.wild.WildRequest;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandAsyncSupport;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandReturns;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TimeUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

final class WildHandler {
	private WildHandler() {
	}

	static int execute(ServerPlayer player) {
		WildCommandSettings settings = ConfigManager.query(WildHandler::settingsFrom);
		if (!settings.enabled()) {
			WildMessages.send(player, "commands.teleport_commands.wild.disabled", ChatFormatting.RED);
			return CommandReturns.FAILED;
		}
		if (settings.minRadius() < WildRequest.MIN_RADIUS || settings.maxRadius() < settings.minRadius()) {
			WildMessages.send(player, "commands.teleport_commands.wild.invalidRadius", ChatFormatting.RED);
			return CommandReturns.FAILED;
		}

		WildService service = TeleportCommands.WILD_SERVICE;
		if (service == null) {
			WildMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}
		if (service.hasCurrentRequest(player.getUUID())) {
			WildMessages.send(player, "commands.teleport_commands.wild.alreadyPending", ChatFormatting.YELLOW);
			return CommandReturns.COMPLETED_SYNC;
		}

		try {
			WildRequest request = new WildRequest(settings.minRadius(), settings.maxRadius(), settings.delayTicks(),
					settings.cooldownMillis(), true);
			CompletableFuture<TeleportStatus> result = service.request(player, request);
			if (result.isDone()) {
				TeleportStatus status = result.join();
				WildMessages.sendStatus(player, status, settings.cooldownSeconds());
				return CommandReturns.forTeleportStatus(status);
			}

			if (settings.delaySeconds() > 0) {
				WildMessages.sendDelayStart(player, settings.delaySeconds());
			} else {
				WildMessages.send(player, "commands.teleport_commands.wild.go", ChatFormatting.AQUA);
			}
			CommandAsyncSupport.whenCompleteForPlayer(player, result, (currentPlayer, status, throwable) -> {
				if (throwable != null) {
					ModConstants.LOGGER.error("Error while executing /wild.", throwable);
					WildMessages.send(currentPlayer, "commands.teleport_commands.common.error",
							ChatFormatting.RED, ChatFormatting.BOLD);
					return;
				}
				WildMessages.sendStatus(currentPlayer, status, settings.cooldownSeconds());
			});
			return CommandReturns.ACCEPTED_ASYNC;
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Error while executing /wild.", exception);
			WildMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}
	}

	private static WildCommandSettings settingsFrom(Config config) {
		int delaySeconds = config.getTeleporting().getDelay();
		int cooldownSeconds = config.getTeleporting().getCooldown();
		return new WildCommandSettings(config.getWild().isEnabled(), config.getWild().getMinRadius(),
				config.getWild().getMaxRadius(), delaySeconds, TimeUtils.secondsToTicks(delaySeconds), cooldownSeconds,
				TimeUtils.secondsToMillis(cooldownSeconds));
	}

	private record WildCommandSettings(boolean enabled, int minRadius, int maxRadius, int delaySeconds,
			int delayTicks, int cooldownSeconds, long cooldownMillis) {
	}
}
