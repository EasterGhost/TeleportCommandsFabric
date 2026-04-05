package org.AndrewElizabeth.teleportcommandsfabric.core.teleport;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;
import static org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG;

public final class TpaService {
	private static final Map<UUID, Request> requestsById = new ConcurrentHashMap<>();
	private static final ScheduledExecutorService REQUEST_EXPIRATION_SCHEDULER = Executors
			.newSingleThreadScheduledExecutor(runnable -> {
				Thread thread = new Thread(runnable, "teleportcommands-TpaCommand-expiration");
				thread.setDaemon(true);
				return thread;
			});
	private static final Map<UUID, ScheduledFuture<?>> requestExpiryTasks = new ConcurrentHashMap<>();

	private TpaService() {
	}

	public static final class Request {
		public final String initPlayer;
		public final String recPlayer;
		public final UUID requestId;
		public final boolean here;

		public Request(String initPlayer, String recPlayer, boolean here) {
			this.initPlayer = initPlayer;
			this.recPlayer = recPlayer;
			this.requestId = UUID.randomUUID();
			this.here = here;
			requestsById.put(requestId, this);
		}
	}

	public static void sendRequest(ServerPlayer fromPlayer, ServerPlayer toPlayer, boolean here) {
		long playerTpaList = getRequests().stream()
				.filter(TpaCommand -> Objects.equals(fromPlayer.getStringUUID(), TpaCommand.initPlayer))
				.filter(TpaCommand -> Objects.equals(toPlayer.getStringUUID(), TpaCommand.recPlayer))
				.count();

		if (fromPlayer == toPlayer) {
			fromPlayer.sendSystemMessage(
					getTranslatedText("commands.teleport_commands.tpa.self", fromPlayer).withStyle(ChatFormatting.AQUA), true);
			return;
		}

		if (playerTpaList >= 1) {
			fromPlayer.sendSystemMessage(getTranslatedText("commands.teleport_commands.tpa.alreadySent", fromPlayer,
					Component.literal(Objects.requireNonNull(toPlayer.getName().getString(),
							"ToPlayer name cannot be null")).withStyle(ChatFormatting.BOLD))
									.withStyle(ChatFormatting.AQUA),
					true);
			return;
		}

		String hereText = here ? "Here" : "";
		Request request = new Request(fromPlayer.getStringUUID(), toPlayer.getStringUUID(), here);

		String receivedFromPlayer = Objects.requireNonNull(fromPlayer.getName().getString(),
				"FromPlayer name cannot be null");
		String sentToPlayer = Objects.requireNonNull(toPlayer.getName().getString(), "ToPlayer name cannot be null");

		fromPlayer.sendSystemMessage(getTranslatedText("commands.teleport_commands.tpa.sent", fromPlayer,
				Component.literal(hereText), Component.literal(sentToPlayer).withStyle(ChatFormatting.BOLD)), true);

		toPlayer.sendSystemMessage(
				getTranslatedText("commands.teleport_commands.tpa.received", toPlayer, Component.literal(hereText),
						Component.literal(receivedFromPlayer).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
								.withStyle(ChatFormatting.AQUA)
								.append("\n")
								.append(getTranslatedText("commands.teleport_commands.tpa.accept", toPlayer)
										.withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
										.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(
												String.format("tpaaccept %s %s", receivedFromPlayer, request.requestId)))))
								.append(" ")
								.append(getTranslatedText("commands.teleport_commands.tpa.deny", toPlayer)
										.withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
										.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(
												String.format("tpadeny %s %s", receivedFromPlayer, request.requestId))))),
				false);

		long requestExpireTimeSeconds = Math.max(0, CONFIG.getTpa().getRequestExpireTime());
		ScheduledFuture<?> expiryTask = REQUEST_EXPIRATION_SCHEDULER.schedule(() -> {
			if (TeleportCommands.SERVER != null) {
				TeleportCommands.SERVER.execute(() -> expireRequest(request, fromPlayer, toPlayer, hereText));
			}
		}, requestExpireTimeSeconds, TimeUnit.SECONDS);
		requestExpiryTasks.put(request.requestId, expiryTask);
	}

	public static void acceptRequest(ServerPlayer recipient, ServerPlayer sender, UUID requestId) {
		if (recipient == sender) {
			recipient.sendSystemMessage(
					getTranslatedText("commands.teleport_commands.tpa.self", recipient).withStyle(ChatFormatting.AQUA), true);
			return;
		}

		Optional<Request> request = findMatchingRequest(recipient, sender, requestId);
		if (request.isEmpty()) {
			recipient.sendSystemMessage(getTranslatedText("commands.teleport_commands.tpa.notFound", recipient)
					.withStyle(ChatFormatting.RED), true);
			return;
		}

		ServerPlayer destinationPlayer = request.get().here ? sender : recipient;
		ServerPlayer teleportedPlayer = request.get().here ? recipient : sender;

		Optional<BlockPos> teleportData = TeleportSafety.getSafeBlockPos(destinationPlayer.blockPosition(),
				destinationPlayer.level());

		boolean teleportSuccess;
		if (teleportData.isPresent()) {
			BlockPos safeBlockPos = teleportData.get();
			Vec3 teleportPos = new Vec3(safeBlockPos.getX() + 0.5, safeBlockPos.getY(), safeBlockPos.getZ() + 0.5);

			teleportSuccess = TeleportService.teleportWithDelayAndCooldown(teleportedPlayer, destinationPlayer.level(),
					teleportPos, false);
		} else {
			teleportSuccess = TeleportService.teleportWithDelayAndCooldown(teleportedPlayer, destinationPlayer.level(),
					destinationPlayer.position(), false);
		}

		if (!teleportSuccess) {
			return;
		}

		recipient.sendSystemMessage(getTranslatedText("commands.teleport_commands.tpa.accepted", recipient)
				.withStyle(ChatFormatting.WHITE), true);
		sender.sendSystemMessage(getTranslatedText("commands.teleport_commands.tpa.accepted", sender)
				.withStyle(ChatFormatting.GREEN), true);
		removeRequest(request.get());
		cancelExpiryTask(request.get().requestId);
	}

	public static void denyRequest(ServerPlayer recipient, ServerPlayer sender, UUID requestId) {
		if (recipient == sender) {
			recipient.sendSystemMessage(
					getTranslatedText("commands.teleport_commands.tpa.self", recipient).withStyle(ChatFormatting.AQUA), true);
			return;
		}

		Optional<Request> request = findMatchingRequest(recipient, sender, requestId);
		if (request.isEmpty()) {
			recipient.sendSystemMessage(getTranslatedText("commands.teleport_commands.tpa.notFound", recipient)
					.withStyle(ChatFormatting.RED), true);
			return;
		}

		removeRequest(request.get());
		cancelExpiryTask(request.get().requestId);

		sender.sendSystemMessage(getTranslatedText("commands.teleport_commands.tpa.denied", sender)
				.withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
		recipient.sendSystemMessage(getTranslatedText("commands.teleport_commands.tpa.denied", recipient)
				.withStyle(ChatFormatting.WHITE), true);
	}

	public static Collection<Request> getRequests() {
		return requestsById.values();
	}

	private static void expireRequest(Request request, ServerPlayer fromPlayer, ServerPlayer toPlayer, String hereText) {
		requestExpiryTasks.remove(request.requestId);

		boolean successful = removeRequest(request);
		if (!successful) {
			return;
		}

		fromPlayer.sendSystemMessage(
				getTranslatedText("commands.teleport_commands.tpa.expired", fromPlayer, Component.literal(hereText))
						.withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
				true);
		toPlayer.sendSystemMessage(
				getTranslatedText("commands.teleport_commands.tpa.expired", toPlayer, Component.literal(hereText))
						.withStyle(ChatFormatting.WHITE),
				true);
	}

	private static void cancelExpiryTask(UUID requestId) {
		ScheduledFuture<?> expiryTask = requestExpiryTasks.remove(requestId);
		if (expiryTask != null) {
			expiryTask.cancel(false);
		}
	}

	private static Optional<Request> findMatchingRequest(ServerPlayer recipient, ServerPlayer sender, UUID requestId) {
		if (requestId == null) {
			return getRequests().stream()
					.filter(TpaCommand -> Objects.equals(sender.getStringUUID(), TpaCommand.initPlayer))
					.filter(TpaCommand -> Objects.equals(recipient.getStringUUID(), TpaCommand.recPlayer))
					.findFirst();
		}
		Request request = requestsById.get(requestId);
		if (request == null) {
			return Optional.empty();
		}
		if (!Objects.equals(sender.getStringUUID(), request.initPlayer)
				|| !Objects.equals(recipient.getStringUUID(), request.recPlayer)) {
			return Optional.empty();
		}
		return Optional.of(request);
	}

	private static boolean removeRequest(Request request) {
		return requestsById.remove(request.requestId, request);
	}
}

