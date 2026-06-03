package org.AndrewElizabeth.teleportcommandsfabric.modules.tpa;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.Config;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TpaService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.Tpa;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.TpaRequest;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class TpaRequestHandler {
	private static final int TICKS_PER_SECOND = 20;
	private static final long MILLIS_PER_SECOND = 1000L;

	private TpaRequestHandler() {
	}

	static int sendRequest(ServerPlayer sender, ServerPlayer target, Tpa.Type type) {
		TpaCommandSettings settings = ConfigManager.query(TpaRequestHandler::settingsFrom);
		if (!settings.enabled()) {
			TpaMessages.send(sender, "commands.teleport_commands.tpa.disabled", ChatFormatting.RED);
			return 1;
		}
		TpaService service = TeleportCommands.TPA_SERVICE;
		if (service == null) {
			TpaMessages.send(sender, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		if (sender.getUUID().equals(target.getUUID())) {
			TpaMessages.send(sender, "commands.teleport_commands.tpa.self", ChatFormatting.AQUA);
			return 0;
		}
		if (service.hasOutgoing(sender.getUUID(), target.getUUID())) {
			TpaMessages.sendAlreadySent(sender, target);
			return 0;
		}

		try {
			TpaRequest request = new TpaRequest(sender.getUUID(), target.getUUID(), type, settings.expiry(),
					settings.delayTicks(), settings.cooldownMillis(), true);
			Tpa.Session session = service.createRequest(request);
			TpaMessages.sendRequestSent(sender, target, type);
			TpaMessages.sendRequestReceived(sender, target, type, session.sessionId());
			return 0;
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Error while sending a TPA request.", exception);
			TpaMessages.send(sender, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
	}

	static int handleResponse(ServerPlayer recipient, ServerPlayer sender, UUID sessionId, boolean accept) {
		TpaCommandSettings settings = ConfigManager.query(TpaRequestHandler::settingsFrom);
		if (!settings.enabled()) {
			TpaMessages.send(recipient, "commands.teleport_commands.tpa.disabled", ChatFormatting.RED);
			return 1;
		}
		TpaService service = TeleportCommands.TPA_SERVICE;
		if (service == null) {
			TpaMessages.send(recipient, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
		if (recipient.getUUID().equals(sender.getUUID())) {
			TpaMessages.send(recipient, "commands.teleport_commands.tpa.self", ChatFormatting.AQUA);
			return 0;
		}

		Optional<Tpa.Session> session = service.findIncoming(recipient.getUUID(), sender.getUUID(), sessionId);
		if (session.isEmpty()) {
			TpaMessages.send(recipient, "commands.teleport_commands.tpa.notFound", ChatFormatting.RED);
			return 0;
		}

		if (!accept) {
			service.remove(session.get().sessionId());
			TpaMessages.sendDenied(recipient, sender);
			return 0;
		}
		return acceptRequest(recipient, sender, session.get(), settings);
	}

	static int handleResponseWithRequestId(ServerPlayer recipient, ServerPlayer sender, String requestId, boolean accept) {
		UUID sessionId;
		try {
			sessionId = UUID.fromString(requestId);
		} catch (IllegalArgumentException exception) {
			TpaMessages.send(recipient, "commands.teleport_commands.tpa.notFound", ChatFormatting.RED);
			return 0;
		}
		return handleResponse(recipient, sender, sessionId, accept);
	}

	private static int acceptRequest(ServerPlayer recipient, ServerPlayer sender, Tpa.Session session,
			TpaCommandSettings settings) {
		MinecraftServer server = recipient.level().getServer();
		try {
			CompletableFuture<TeleportStatus> result = TeleportCommands.TPA_SERVICE.acceptRequest(server, session.sessionId());
			if (result.isDone()) {
				sendFailureStatus(recipient, sender, session, result.join(), settings.cooldownSeconds());
				return 0;
			}
			TpaMessages.sendAccepted(recipient, sender);
			result.whenComplete((status, throwable) -> server.execute(() -> {
				ServerPlayer currentRecipient = server.getPlayerList().getPlayer(recipient.getUUID());
				ServerPlayer currentSender = server.getPlayerList().getPlayer(sender.getUUID());
				if (throwable != null) {
					ModConstants.LOGGER.error("Error while accepting a TPA request.", throwable);
					if (currentRecipient != null) {
						TpaMessages.send(currentRecipient, "commands.teleport_commands.common.error",
								ChatFormatting.RED, ChatFormatting.BOLD);
					}
					return;
				}
				sendFailureStatus(currentRecipient, currentSender, session, status, settings.cooldownSeconds());
			}));
			return 0;
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Error while accepting a TPA request.", exception);
			TpaMessages.send(recipient, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}
	}

	private static void sendFailureStatus(ServerPlayer recipient, ServerPlayer sender, Tpa.Session session,
			TeleportStatus status, int cooldownSeconds) {
		if (status == TeleportStatus.SUCCESS || status == TeleportStatus.CANCELLED) {
			return;
		}
		ServerPlayer playerToMove = session.type() == Tpa.Type.TPA ? sender : recipient;
		TpaMessages.sendStatus(playerToMove == null ? recipient : playerToMove, status, cooldownSeconds);
	}

	private static TpaCommandSettings settingsFrom(Config config) {
		int delaySeconds = config.getTeleporting().getDelay();
		int cooldownSeconds = config.getTeleporting().getCooldown();
		return new TpaCommandSettings(config.getTpa().isEnabled(), config.getTpa().getRequestExpireTime(),
				delaySeconds * TICKS_PER_SECOND, cooldownSeconds, cooldownSeconds * MILLIS_PER_SECOND);
	}

	private record TpaCommandSettings(boolean enabled, Duration expiry, int delayTicks, int cooldownSeconds,
			long cooldownMillis) {
	}
}
