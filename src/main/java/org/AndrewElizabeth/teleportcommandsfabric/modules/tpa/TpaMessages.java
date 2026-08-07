package org.AndrewElizabeth.teleportcommandsfabric.modules.tpa;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.Tpa;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.MessageSupport;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.TpaTrustDecision;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.TpaTrustEntry;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

final class TpaMessages {
	private TpaMessages() {
	}

	static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		MessageSupport.send(player, key, formatting);
	}

	static void sendRequestSent(ServerPlayer sender, ServerPlayer target, Tpa.Type type) {
		sender.sendSystemMessage(TranslationHelper.getTranslatedText("commands.teleport_commands.tpa.sent", sender,
				hereText(type), Component.literal(target.getName().getString()).withStyle(ChatFormatting.BOLD))
				.withStyle(ChatFormatting.AQUA), true);
	}

	static void sendAlreadySent(ServerPlayer sender, ServerPlayer target) {
		sender.sendSystemMessage(TranslationHelper.getTranslatedText("commands.teleport_commands.tpa.alreadySent", sender,
				Component.literal(target.getName().getString()).withStyle(ChatFormatting.BOLD))
				.withStyle(ChatFormatting.AQUA), true);
	}

	static void sendRequestReceived(ServerPlayer sender, ServerPlayer target, Tpa.Type type, UUID sessionId) {
		String senderName = sender.getName().getString();
		target.sendSystemMessage(TranslationHelper.getTranslatedText("commands.teleport_commands.tpa.received", target,
				hereText(type), Component.literal(senderName).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
				.withStyle(ChatFormatting.AQUA)
				.append("\n")
				.append(TranslationHelper.getTranslatedText("commands.teleport_commands.tpa.accept", target)
						.withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
						.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(
								"tpaaccept " + senderName + " " + sessionId))))
				.append(" ")
				.append(TranslationHelper.getTranslatedText("commands.teleport_commands.tpa.deny", target)
						.withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
						.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(
								"tpadeny " + senderName + " " + sessionId)))),
				false);
	}

	static void sendAccepted(ServerPlayer recipient, ServerPlayer sender) {
		send(recipient, "commands.teleport_commands.tpa.accepted", ChatFormatting.WHITE);
		send(sender, "commands.teleport_commands.tpa.accepted", ChatFormatting.GREEN);
	}

	static void sendDenied(ServerPlayer recipient, ServerPlayer sender) {
		send(sender, "commands.teleport_commands.tpa.denied", ChatFormatting.RED, ChatFormatting.BOLD);
		send(recipient, "commands.teleport_commands.tpa.denied", ChatFormatting.WHITE);
	}

	static void sendTrustDenied(ServerPlayer sender, ServerPlayer target, Tpa.Type type) {
		sender.sendSystemMessage(TranslationHelper.getTranslatedText("commands.teleport_commands.tpa.trustDenied",
				sender, hereText(type), Component.literal(target.getName().getString()).withStyle(ChatFormatting.BOLD))
				.withStyle(ChatFormatting.RED), true);
	}

	static void sendTrustedAccepted(ServerPlayer sender, ServerPlayer target, Tpa.Type type) {
		sender.sendSystemMessage(TranslationHelper.getTranslatedText("commands.teleport_commands.tpa.trustedAcceptedSender",
				sender, hereText(type), Component.literal(target.getName().getString()).withStyle(ChatFormatting.BOLD))
				.withStyle(ChatFormatting.GREEN), true);
		target.sendSystemMessage(TranslationHelper.getTranslatedText("commands.teleport_commands.tpa.trustedAcceptedTarget",
				target, hereText(type), Component.literal(sender.getName().getString()).withStyle(ChatFormatting.BOLD))
				.withStyle(ChatFormatting.WHITE), true);
	}

	static void sendTrustUpdated(ServerPlayer owner, TpaTrustTarget target, Tpa.Type type, TpaTrustDecision decision) {
		owner.sendSystemMessage(TranslationHelper.getTranslatedText("commands.teleport_commands.tpa.trustUpdated",
				owner, targetText(owner, target), typeText(owner, type), decisionText(owner, decision))
				.withStyle(ChatFormatting.GREEN), false);
	}

	static void sendTrustUpdated(ServerPlayer owner, TpaTrustTarget target, TpaTrustDecision tpaDecision,
			TpaTrustDecision tpaHereDecision) {
		owner.sendSystemMessage(TranslationHelper.getTranslatedText("commands.teleport_commands.tpa.trustUpdatedBoth",
				owner, targetText(owner, target), decisionText(owner, tpaDecision), decisionText(owner, tpaHereDecision))
				.withStyle(ChatFormatting.GREEN), false);
	}

	static void sendTrustStatus(ServerPlayer owner, TpaTrustTarget target, TpaTrustEntry entry) {
		String key = target.all()
				? "commands.teleport_commands.tpa.trustStatus"
				: "commands.teleport_commands.tpa.trustOverrideStatus";
		owner.sendSystemMessage(TranslationHelper.getTranslatedText(key,
				owner, targetText(owner, target), decisionText(owner, entry.tpa()), decisionText(owner, entry.tpaHere()))
				.withStyle(ChatFormatting.AQUA), false);
	}

	static void sendExpired(ServerPlayer sender, ServerPlayer target, Tpa.Type type) {
		if (sender != null) {
			sender.sendSystemMessage(TranslationHelper.getTranslatedText("commands.teleport_commands.tpa.expired",
					sender, hereText(type)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
		}
		if (target != null) {
			target.sendSystemMessage(TranslationHelper.getTranslatedText("commands.teleport_commands.tpa.expired",
					target, hereText(type)).withStyle(ChatFormatting.WHITE), true);
		}
	}

	static void sendStatus(ServerPlayer player, TeleportStatus status, int cooldownSeconds) {
		if (player == null || status == null || status == TeleportStatus.SUCCESS || status == TeleportStatus.CANCELLED) {
			return;
		}
		switch (status) {
		case COOLDOWN -> MessageSupport.sendCooldown(player, cooldownSeconds);
		case TARGET_UNAVAILABLE, PLAYER_DISCONNECTED -> send(player, "commands.teleport_commands.tpa.notFound",
				ChatFormatting.RED);
		case CANCELLED_BY_EVENT -> send(player, "commands.teleport_commands.common.error",
				ChatFormatting.RED, ChatFormatting.BOLD);
		default -> send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}

	private static MutableComponent hereText(Tpa.Type type) {
		return Component.literal(type == Tpa.Type.TPAHERE ? "Here" : "");
	}

	private static MutableComponent targetText(ServerPlayer owner, TpaTrustTarget target) {
		if (target.all()) {
			return TranslationHelper.getTranslatedText("commands.teleport_commands.tpa.trustTargetAll", owner);
		}
		return Component.literal(target.displayName()).withStyle(ChatFormatting.BOLD);
	}

	private static MutableComponent typeText(ServerPlayer owner, Tpa.Type type) {
		return TranslationHelper.getTranslatedText(type == Tpa.Type.TPAHERE
				? "commands.teleport_commands.tpa.trustTypeTpahere"
				: "commands.teleport_commands.tpa.trustTypeTpa", owner);
	}

	private static MutableComponent decisionText(ServerPlayer owner, TpaTrustDecision decision) {
		TpaTrustDecision safeDecision = decision == null ? TpaTrustDecision.DEFAULT : decision;
		return TranslationHelper.getTranslatedText("commands.teleport_commands.tpa.trustDecision." + safeDecision.serializedName(), owner);
	}
}
