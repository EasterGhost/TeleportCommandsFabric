package org.AndrewElizabeth.teleportcommandsfabric.modules.common;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.TargetTeleportOptions;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.TeleportRequest;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

public final class TargetTeleportCommandSupport {
	private TargetTeleportCommandSupport() {
	}

	public static boolean submit(ServerPlayer player, TeleportTarget target, Settings settings,
			String startedKey, String errorKey, String errorLogMessage, String forceCommand,
			StatusSender statusSender) {
		TeleportService service = TeleportCommands.TELEPORT_SERVICE;
		if (service == null) {
			MessageSupport.send(player, errorKey, ChatFormatting.RED, ChatFormatting.BOLD);
			return false;
		}

		TargetTeleportOptions options = TargetTeleportOptions.builder()
				.delayTicks(settings.delayTicks())
				.cooldownMillis(settings.cooldownMillis())
				.safetyEnabled(settings.safetyEnabled())
				.recordPrevious(settings.recordPrevious())
				.build();
		TeleportRequest request = TeleportRequest.resolved(target, options);

		try {
			CompletableFuture<TeleportStatus> result = service.request(player, request);
			if (result.isDone()) {
				statusSender.send(player, result.join(), settings.cooldownSeconds(), forceCommand);
				return true;
			}
			if (settings.delaySeconds() > 0) {
				MessageSupport.sendDelayStart(player, settings.delaySeconds());
			} else {
				MessageSupport.send(player, startedKey, ChatFormatting.AQUA);
			}
			CommandAsyncSupport.whenCompleteForPlayer(player, result, (currentPlayer, status, throwable) -> {
				if (throwable != null) {
					ModConstants.LOGGER.error(errorLogMessage, throwable);
					MessageSupport.send(currentPlayer, errorKey, ChatFormatting.RED, ChatFormatting.BOLD);
					return;
				}
				statusSender.send(currentPlayer, status, settings.cooldownSeconds(), forceCommand);
			});
			return true;
		} catch (Exception exception) {
			ModConstants.LOGGER.error(errorLogMessage, exception);
			MessageSupport.send(player, errorKey, ChatFormatting.RED, ChatFormatting.BOLD);
			return false;
		}
	}

	public record Settings(int delaySeconds, int delayTicks, int cooldownSeconds, long cooldownMillis,
			boolean safetyEnabled, boolean recordPrevious) {
	}

	@FunctionalInterface
	public interface StatusSender {
		void send(ServerPlayer player, TeleportStatus status, int cooldownSeconds, String forceCommand);
	}
}
