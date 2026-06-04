package org.AndrewElizabeth.teleportcommandsfabric.modules.rtp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.Config;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.RtpService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.rtp.RtpRequest;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TimeUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class RtpCommand {
	private RtpCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(buildRtpCommand("rtp"));
		dispatcher.register(buildRtpCommand("wild"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRtpCommand(String commandName) {
		return Commands.literal(commandName)
				.requires(source -> source.getPlayer() != null)
				.executes(context -> execute(context.getSource().getPlayerOrException()));
	}

	private static int execute(ServerPlayer player) {
		RtpCommandSettings settings = ConfigManager.query(RtpCommand::settingsFrom);
		if (!settings.enabled()) {
			RtpMessages.send(player, "commands.teleport_commands.rtp.disabled", ChatFormatting.RED);
			return 1;
		}
		if (settings.maxRadius() < 1) {
			RtpMessages.send(player, "commands.teleport_commands.rtp.invalidRadius", ChatFormatting.RED);
			return 1;
		}

		RtpService service = TeleportCommands.RTP_SERVICE;
		if (service == null) {
			RtpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}

		try {
			RtpRequest request = new RtpRequest(settings.minRadius(), settings.maxRadius(),
					RtpService.DEFAULT_MAX_ATTEMPTS, settings.delayTicks(), settings.cooldownMillis(), true);
			CompletableFuture<TeleportStatus> result = service.request(player, request);
			if (result.isDone()) {
				RtpMessages.sendStatus(player, result.join(), settings.cooldownSeconds());
				return 0;
			} else if (settings.delaySeconds() > 0) {
				RtpMessages.sendDelayStart(player, settings.delaySeconds());
			} else {
				RtpMessages.send(player, "commands.teleport_commands.rtp.go", ChatFormatting.AQUA);
			}
			MinecraftServer server = player.level().getServer();
			UUID playerUuid = player.getUUID();
			result.whenComplete((status, throwable) -> {
				if (throwable != null) {
					ModConstants.LOGGER.error("Error while executing /rtp.", throwable);
				}
				server.execute(() -> sendResult(server, playerUuid, settings.cooldownSeconds(), status, throwable));
			});
			return 0;
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Error while executing /rtp.", exception);
			RtpMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
	}

	private static RtpCommandSettings settingsFrom(Config config) {
		int delaySeconds = config.getTeleporting().getDelay();
		int cooldownSeconds = config.getTeleporting().getCooldown();
		return new RtpCommandSettings(config.getRtp().isEnabled(), config.getRtp().getMinRadius(),
				config.getRtp().getMaxRadius(), delaySeconds, TimeUtils.secondsToTicks(delaySeconds),
				cooldownSeconds, TimeUtils.secondsToMillis(cooldownSeconds));
	}

	private static void sendResult(MinecraftServer server, UUID playerUuid, int cooldownSeconds, TeleportStatus status,
			Throwable throwable) {
		ServerPlayer currentPlayer = server.getPlayerList().getPlayer(playerUuid);
		if (currentPlayer == null) {
			return;
		}
		if (throwable != null) {
			RtpMessages.send(currentPlayer, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return;
		}
		RtpMessages.sendStatus(currentPlayer, status, cooldownSeconds);
	}

	private record RtpCommandSettings(boolean enabled, int minRadius, int maxRadius, int delaySeconds, int delayTicks,
			int cooldownSeconds, long cooldownMillis) {
	}
}
