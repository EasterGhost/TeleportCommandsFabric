package org.AndrewElizabeth.teleportcommandsfabric.modules.tpa;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.Tpa;
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
		if (player != null) {
			player.sendSystemMessage(TranslationHelper.getTranslatedText(key, player).withStyle(formatting), true);
		}
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
		case COOLDOWN -> player.sendSystemMessage(TranslationHelper.getTranslatedText(
				"commands.teleport_commands.common.cooldown", player,
				Component.literal(String.valueOf(cooldownSeconds))).withStyle(ChatFormatting.YELLOW), true);
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
}
