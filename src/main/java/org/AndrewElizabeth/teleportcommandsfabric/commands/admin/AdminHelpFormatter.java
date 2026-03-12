package org.AndrewElizabeth.teleportcommandsfabric.commands.admin;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class AdminHelpFormatter {

	private AdminHelpFormatter() {
	}

	static MutableComponent build(CommandSourceStack source) {
		MutableComponent message = Component.empty();

		append(message, AdminMessages.t(source,
				"commands.teleport_commands.admin.help.title",
				Component.literal(Constants.VERSION)), ChatFormatting.AQUA, true);
		append(message, AdminMessages.t(source,
				"commands.teleport_commands.admin.help.section.admin"), ChatFormatting.GOLD, true);
		append(message, Component.literal("/teleportcommands help"), ChatFormatting.YELLOW, false);
		append(message, Component.literal("/teleportcommands status"), ChatFormatting.YELLOW, false);
		append(message, Component.literal("/teleportcommands reload"), ChatFormatting.YELLOW, false);
		append(message, Component.literal("/teleportcommands enable <back|home|tpa|warp|worldspawn|rtp|xaero>"),
				ChatFormatting.YELLOW, false);
		append(message, Component.literal("/teleportcommands disable <back|home|tpa|warp|worldspawn|rtp|xaero>"),
				ChatFormatting.YELLOW, false);

		append(message, Component.literal(""), ChatFormatting.WHITE, false);
		append(message, AdminMessages.t(source,
				"commands.teleport_commands.admin.help.section.config"), ChatFormatting.GOLD, true);
		append(message, Component.literal("/teleportcommands config teleporting delay <seconds>"),
				ChatFormatting.YELLOW, false);
		append(message, Component.literal("/teleportcommands config teleporting cooldown <seconds>"),
				ChatFormatting.YELLOW, false);
		append(message, Component.literal("/teleportcommands config back deleteAfterTeleport <true|false>"),
				ChatFormatting.YELLOW, false);
		append(message, Component.literal("/teleportcommands config home max <count>"),
				ChatFormatting.YELLOW, false);
		append(message, Component.literal("/teleportcommands config home deleteInvalid <true|false>"),
				ChatFormatting.YELLOW, false);
		append(message, Component.literal("/teleportcommands config tpa expireTime <seconds>"),
				ChatFormatting.YELLOW, false);
		append(message, Component.literal("/teleportcommands config warp max <count>"),
				ChatFormatting.YELLOW, false);
		append(message, Component.literal("/teleportcommands config warp deleteInvalid <true|false>"),
				ChatFormatting.YELLOW, false);
		append(message, Component.literal("/teleportcommands config worldspawn world <worldId>"),
				ChatFormatting.YELLOW, false);
		append(message, Component.literal("/teleportcommands config rtp radius <blocks>"),
				ChatFormatting.YELLOW, false);
		append(message, Component.literal("/teleportcommands config xaero syncIntervalSeconds <seconds>"),
				ChatFormatting.YELLOW, false);

		return message;
	}

	private static void append(MutableComponent dst, MutableComponent line, ChatFormatting color, boolean bold) {
		MutableComponent text = line.copy().append("\n");
		dst.append(bold ? text.withStyle(color, ChatFormatting.BOLD) : text.withStyle(color));
	}
}
