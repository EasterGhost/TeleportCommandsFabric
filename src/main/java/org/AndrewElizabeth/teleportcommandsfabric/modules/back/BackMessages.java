package org.AndrewElizabeth.teleportcommandsfabric.modules.back;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.MessageSupport;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

final class BackMessages {
	private static final String COMMAND_BACK_TP = "back tp";
	private static final String DISPLAY_COMMAND_BACK_TP = "/back tp";

	private BackMessages() {
	}

	static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		MessageSupport.send(player, key, formatting);
	}

	static void sendDelayStart(ServerPlayer player, int delaySeconds) {
		MessageSupport.sendDelayStart(player, delaySeconds);
	}

	static void sendTryBackTpPrompt(ServerPlayer player) {
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

	static void sendStatus(ServerPlayer player, TeleportStatus status, int cooldownSeconds,
			String forceCommand) {
		MessageSupport.sendTeleportStatus(player, status, cooldownSeconds, forceCommand,
				"commands.teleport_commands.common.noLocation", ChatFormatting.RED);
	}
}
