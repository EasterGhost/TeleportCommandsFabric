package org.AndrewElizabeth.teleportcommandsfabric.modules.tpa;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.Config;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TpaService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.Tpa;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.TpaRequest;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandReturns;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.TpaTrustDecision;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TimeUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class TpaRequestHandler {
	private TpaRequestHandler() {
	}

	static int sendRequest(ServerPlayer sender, ServerPlayer target, Tpa.Type type) {
		TpaCommandSettings settings = ConfigManager.query(TpaRequestHandler::settingsFrom);
		if (!settings.enabled()) {
			TpaMessages.send(sender, "commands.teleport_commands.tpa.disabled", ChatFormatting.RED);
			return CommandReturns.FAILED;
		}
		TpaService service = TeleportCommands.TPA_SERVICE;
		if (service == null) {
			TpaMessages.send(sender, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}
		if (sender.getUUID().equals(target.getUUID())) {
			TpaMessages.send(sender, "commands.teleport_commands.tpa.self", ChatFormatting.AQUA);
			return CommandReturns.COMPLETED_SYNC;
		}
		if (service.hasOutgoing(sender.getUUID(), target.getUUID())
				|| service.hasPendingRequest(sender.getUUID(), target.getUUID(), type)) {
			TpaMessages.sendAlreadySent(sender, target);
			return CommandReturns.COMPLETED_SYNC;
		}

		PlayerProfileManager profileManager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (profileManager == null) {
			TpaMessages.send(sender, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}

		MinecraftServer server = sender.level().getServer();
		UUID senderUuid = sender.getUUID();
		UUID targetUuid = target.getUUID();
		TpaRequest request = new TpaRequest(senderUuid, targetUuid, type, settings.expiry(),
				settings.delayTicks(), settings.cooldownMillis(), true);
		profileManager.query(targetUuid, profile -> profile.resolveTpaTrust(senderUuid, type))
				.whenComplete((decision, throwable) -> server.execute(() -> {
					ServerPlayer currentSender = server.getPlayerList().getPlayer(senderUuid);
					ServerPlayer currentTarget = server.getPlayerList().getPlayer(targetUuid);
					if (currentSender == null || currentTarget == null) {
						return;
					}
					if (throwable != null) {
						ModConstants.LOGGER.error("Error while reading TPA trust settings.", throwable);
						TpaMessages.send(currentSender, "commands.teleport_commands.common.error",
								ChatFormatting.RED, ChatFormatting.BOLD);
						return;
					}
					if (service.hasOutgoing(senderUuid, targetUuid)
							|| service.hasPendingRequest(senderUuid, targetUuid, type)) {
						TpaMessages.sendAlreadySent(currentSender, currentTarget);
						return;
					}
					handleTrustDecision(service, server, currentSender, currentTarget, request, decision, settings);
				}));
		return CommandReturns.ACCEPTED_ASYNC;
	}

	private static void handleTrustDecision(TpaService service, MinecraftServer server, ServerPlayer sender,
			ServerPlayer target, TpaRequest request, TpaTrustDecision decision, TpaCommandSettings settings) {
		TpaTrustDecision safeDecision = decision == null ? TpaTrustDecision.DEFAULT : decision;
		if (safeDecision == TpaTrustDecision.DENY) {
			TpaMessages.sendTrustDenied(sender, target, request.type());
			return;
		}
		if (safeDecision == TpaTrustDecision.ACCEPT) {
			executeTrustedRequest(service, server, sender, target, request, settings);
			return;
		}
		createPendingRequest(service, sender, target, request);
	}

	private static void createPendingRequest(TpaService service, ServerPlayer sender, ServerPlayer target,
			TpaRequest request) {
		try {
			Tpa.Session session = service.createRequest(request);
			TpaMessages.sendRequestSent(sender, target, request.type());
			TpaMessages.sendRequestReceived(sender, target, request.type(), session.sessionId());
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Error while sending a TPA request.", exception);
			TpaMessages.send(sender, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	private static void executeTrustedRequest(TpaService service, MinecraftServer server, ServerPlayer sender,
			ServerPlayer target, TpaRequest request, TpaCommandSettings settings) {
		try {
			CompletableFuture<TeleportStatus> result = service.executeTrustedRequest(server, request);
			if (result.isDone()) {
				sendImmediateTrustedStatus(sender, target, request, result.join(), settings.cooldownSeconds());
				return;
			}
			TpaMessages.sendTrustedAccepted(sender, target, request.type());
			result.whenComplete((status, throwable) -> server.execute(() -> {
				ServerPlayer currentSender = server.getPlayerList().getPlayer(sender.getUUID());
				ServerPlayer currentTarget = server.getPlayerList().getPlayer(target.getUUID());
				if (throwable != null) {
					ModConstants.LOGGER.error("Error while executing a trusted TPA request.", throwable);
					if (currentSender != null) {
						TpaMessages.send(currentSender, "commands.teleport_commands.common.error",
								ChatFormatting.RED, ChatFormatting.BOLD);
					}
					return;
				}
				sendFailureStatus(currentTarget, currentSender, trustedSession(request), status, settings.cooldownSeconds());
			}));
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Error while executing a trusted TPA request.", exception);
			TpaMessages.send(sender, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	private static void sendImmediateTrustedStatus(ServerPlayer sender, ServerPlayer target, TpaRequest request,
			TeleportStatus status, int cooldownSeconds) {
		if (status == TeleportStatus.SUCCESS) {
			TpaMessages.sendTrustedAccepted(sender, target, request.type());
			return;
		}
		sendFailureStatus(target, sender, trustedSession(request), status, cooldownSeconds);
	}

	static int handleResponse(ServerPlayer recipient, ServerPlayer sender, UUID sessionId, boolean accept) {
		TpaCommandSettings settings = ConfigManager.query(TpaRequestHandler::settingsFrom);
		if (!settings.enabled()) {
			TpaMessages.send(recipient, "commands.teleport_commands.tpa.disabled", ChatFormatting.RED);
			return CommandReturns.FAILED;
		}
		TpaService service = TeleportCommands.TPA_SERVICE;
		if (service == null) {
			TpaMessages.send(recipient, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}
		if (recipient.getUUID().equals(sender.getUUID())) {
			TpaMessages.send(recipient, "commands.teleport_commands.tpa.self", ChatFormatting.AQUA);
			return CommandReturns.COMPLETED_SYNC;
		}

		Optional<Tpa.Session> session = service.findIncoming(recipient.getUUID(), sender.getUUID(), sessionId);
		if (session.isEmpty()) {
			TpaMessages.send(recipient, "commands.teleport_commands.tpa.notFound", ChatFormatting.RED);
			return CommandReturns.COMPLETED_SYNC;
		}

		if (!accept) {
			service.remove(session.get().sessionId());
			TpaMessages.sendDenied(recipient, sender);
			return CommandReturns.COMPLETED_SYNC;
		}
		return acceptRequest(recipient, sender, session.get(), settings);
	}

	static int handleResponseWithRequestId(ServerPlayer recipient, ServerPlayer sender, String requestId, boolean accept) {
		UUID sessionId;
		try {
			sessionId = UUID.fromString(requestId);
		} catch (IllegalArgumentException exception) {
			TpaMessages.send(recipient, "commands.teleport_commands.tpa.notFound", ChatFormatting.RED);
			return CommandReturns.COMPLETED_SYNC;
		}
		return handleResponse(recipient, sender, sessionId, accept);
	}

	private static int acceptRequest(ServerPlayer recipient, ServerPlayer sender, Tpa.Session session,
			TpaCommandSettings settings) {
		MinecraftServer server = recipient.level().getServer();
		try {
			CompletableFuture<TeleportStatus> result = TeleportCommands.TPA_SERVICE.acceptRequest(server, session.sessionId());
			if (result.isDone()) {
				TeleportStatus status = result.join();
				sendImmediateStatus(recipient, sender, session, status, settings.cooldownSeconds());
				return CommandReturns.forTeleportStatus(status);
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
			return CommandReturns.ACCEPTED_ASYNC;
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Error while accepting a TPA request.", exception);
			TpaMessages.send(recipient, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
			return CommandReturns.FAILED;
		}
	}

	private static void sendImmediateStatus(ServerPlayer recipient, ServerPlayer sender, Tpa.Session session,
			TeleportStatus status, int cooldownSeconds) {
		if (status == TeleportStatus.SUCCESS) {
			TpaMessages.sendAccepted(recipient, sender);
			return;
		}
		sendFailureStatus(recipient, sender, session, status, cooldownSeconds);
	}

	private static void sendFailureStatus(ServerPlayer recipient, ServerPlayer sender, Tpa.Session session,
			TeleportStatus status, int cooldownSeconds) {
		if (status == TeleportStatus.SUCCESS || status == TeleportStatus.CANCELLED) {
			return;
		}
		ServerPlayer playerToMove = session.type() == Tpa.Type.TPA ? sender : recipient;
		TpaMessages.sendStatus(playerToMove == null ? recipient : playerToMove, status, cooldownSeconds);
	}

	private static Tpa.Session trustedSession(TpaRequest request) {
		return new Tpa.Session(UUID.randomUUID(), request.senderUuid(), request.targetUuid(), request.type(),
				Long.MAX_VALUE, request.delayTicks(), request.cooldownMillis(), request.recordPrevious());
	}

	private static TpaCommandSettings settingsFrom(Config config) {
		int delaySeconds = config.getTeleporting().getDelay();
		int cooldownSeconds = config.getTeleporting().getCooldown();
		return new TpaCommandSettings(config.getTpa().isEnabled(), config.getTpa().getRequestExpireTime(),
				TimeUtils.secondsToTicks(delaySeconds), cooldownSeconds, TimeUtils.secondsToMillis(cooldownSeconds));
	}

	private record TpaCommandSettings(boolean enabled, Duration expiry, int delayTicks, int cooldownSeconds,
			long cooldownMillis) {
	}
}
