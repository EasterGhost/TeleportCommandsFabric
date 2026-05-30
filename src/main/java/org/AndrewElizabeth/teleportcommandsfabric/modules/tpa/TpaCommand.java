package org.AndrewElizabeth.teleportcommandsfabric.modules.tpa;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.Config;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TpaService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.Tpa;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.TpaRequest;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TpaCommand {
	private static final int TICKS_PER_SECOND = 20;
	private static final long MILLIS_PER_SECOND = 1000L;
	private static final TpaSuggestionProvider REQUEST_SUGGESTIONS = new TpaSuggestionProvider();

	private TpaCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(buildRequestNode("tpa", Tpa.Type.TPA));
		dispatcher.register(buildRequestNode("tpahere", Tpa.Type.TPAHERE));
		dispatcher.register(buildResponseNode("tpaaccept", true));
		dispatcher.register(buildResponseNode("tpadeny", false));
	}

	public static void sendExpired(MinecraftServer server, Tpa.Session session) {
		if (server == null || session == null) {
			return;
		}
		ServerPlayer sender = server.getPlayerList().getPlayer(session.sender());
		ServerPlayer target = server.getPlayerList().getPlayer(session.target());
		TpaMessages.sendExpired(sender, target, session.type());
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRequestNode(String literal, Tpa.Type type) {
		return Commands.literal(literal)
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("player", EntityArgument.player())
						.executes(context -> sendRequest(context.getSource().getPlayerOrException(),
								EntityArgument.getPlayer(context, "player"), type)));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildResponseNode(String literal, boolean accept) {
		return Commands.literal(literal)
				.requires(source -> source.getPlayer() != null)
				.then(Commands.argument("player", EntityArgument.player())
						.suggests(REQUEST_SUGGESTIONS)
						.executes(context -> handleResponse(context.getSource().getPlayerOrException(),
								EntityArgument.getPlayer(context, "player"), null, accept))
						.then(Commands.argument("requestId", StringArgumentType.word())
								.executes(context -> handleResponse(context.getSource().getPlayerOrException(),
										EntityArgument.getPlayer(context, "player"),
										parseRequestId(StringArgumentType.getString(context, "requestId")), accept))));
	}

	private static int sendRequest(ServerPlayer sender, ServerPlayer target, Tpa.Type type) {
		TpaCommandSettings settings = ConfigManager.query(TpaCommand::settingsFrom);
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

	private static int handleResponse(ServerPlayer recipient, ServerPlayer sender, UUID sessionId, boolean accept) {
		TpaCommandSettings settings = ConfigManager.query(TpaCommand::settingsFrom);
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

	private static UUID parseRequestId(String requestId) {
		try {
			return UUID.fromString(requestId);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
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
