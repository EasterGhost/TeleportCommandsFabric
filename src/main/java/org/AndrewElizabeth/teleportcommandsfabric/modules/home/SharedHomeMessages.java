package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeKey;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.MessageSupport;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

final class SharedHomeMessages {
	private SharedHomeMessages() {
	}

	static void send(ServerPlayer player, String key, ChatFormatting... formatting) {
		MessageSupport.send(player, key, formatting);
	}

	static void sendLimitReached(ServerPlayer player, int maximum) {
		player.sendSystemMessage(MessageSupport.translated(player,
				"commands.teleport_commands.sharedhome.limitReached",
				Component.literal(Integer.toString(maximum))).withStyle(ChatFormatting.RED), true);
	}

	static void sendBroadcastCooldown(ServerPlayer player, long remainingSeconds) {
		player.sendSystemMessage(MessageSupport.translated(player,
				"commands.teleport_commands.sharedhome.broadcastCooldown",
				Component.literal(Long.toString(remainingSeconds))).withStyle(ChatFormatting.YELLOW), true);
	}

	static void sendBroadcast(ServerPlayer recipient, String ownerName, String homeName, SharedHomeKey key) {
		MutableComponent message = MessageSupport.translated(recipient,
				"commands.teleport_commands.sharedhome.announcement",
				Component.literal(ownerName).withStyle(ChatFormatting.AQUA),
				Component.literal(homeName).withStyle(ChatFormatting.GOLD));
		message.append(" ");
		message.append(MessageSupport.translated(recipient, "commands.teleport_commands.sharedhome.subscribe")
				.withStyle(ChatFormatting.GREEN)
				.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(
						"teleportcommandsfabric:sharedhomeui subscribe " + key.ownerUuid() + " " + key.homeUuid()))));
		recipient.sendSystemMessage(message, false);
	}
}
