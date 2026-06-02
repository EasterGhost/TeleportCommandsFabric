package org.AndrewElizabeth.teleportcommandsfabric.modules.back;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.MessageSupport;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

public final class BackMessages {
	private static final String COMMAND_BACK_TP = "back tp";
	private static final String DISPLAY_COMMAND_BACK_TP = "/back tp";

	private BackMessages() {
	}

	public static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		MessageSupport.send(player, key, formatting);
	}

	public static void sendDelayStart(ServerPlayer player, int delaySeconds) {
		MessageSupport.sendDelayStart(player, delaySeconds);
	}

	public static void sendTryBackTpPrompt(ServerPlayer player) {
		if (player == null) {
			return;
		}
		player.sendSystemMessage(Component.empty()
				.append(TranslationHelper.getTranslatedText("commands.teleport_commands.back.tryTp", player)
						.withStyle(ChatFormatting.YELLOW))
				.append(" ")
				.append(Component.literal("[" + DISPLAY_COMMAND_BACK_TP + "] ?")
						.withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)
						.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(COMMAND_BACK_TP))
								.withHoverEvent(new HoverEvent.ShowText(Component.literal(DISPLAY_COMMAND_BACK_TP))))),
				false);
	}

	public static void sendUnsafeTeleportPrompt(ServerPlayer player, String command) {
		MessageSupport.sendUnsafeTeleportPrompt(player, command);
	}

	public static void sendStatus(ServerPlayer player, TeleportStatus status, int cooldownSeconds,
			String forceCommand) {
		if (player == null || status == null || status == TeleportStatus.SUCCESS || status == TeleportStatus.CANCELLED) {
			return;
		}
		switch (status) {
		case COOLDOWN -> MessageSupport.sendCooldown(player, cooldownSeconds);
		case NO_SAFE_POSITION -> sendUnsafeTeleportPrompt(player, forceCommand);
		case TARGET_UNAVAILABLE -> send(player, "commands.teleport_commands.common.noLocation", ChatFormatting.RED);
		case PLAYER_DISCONNECTED -> {
		}
		default -> send(player, "commands.teleport_commands.common.error", ChatFormatting.RED, ChatFormatting.BOLD);
		}
	}
}
