package org.AndrewElizabeth.teleportcommandsfabric.modules.admin;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class AdminHelpFormatter {
	private static final String ROOT_COMMAND = "/tpc";
	private static final String MODULE_LIST = String.join("|", AdminModuleRegistry.allNames());

	private AdminHelpFormatter() {
	}

	static MutableComponent build(CommandSourceStack source) {
		MutableComponent message = Component.empty();

		append(message, AdminMessages.t(source, "commands.teleport_commands.admin.help.title",
				Component.literal(ModConstants.VERSION)), ChatFormatting.AQUA, true);
		append(message, AdminMessages.t(source,
				"commands.teleport_commands.admin.help.section.admin"), ChatFormatting.GOLD, true);
		append(message, Component.literal(ROOT_COMMAND + " help"), ChatFormatting.YELLOW, false);
		append(message, Component.literal(ROOT_COMMAND + " status"), ChatFormatting.YELLOW, false);
		append(message, Component.literal(ROOT_COMMAND + " reload"), ChatFormatting.YELLOW, false);
		append(message, Component.literal(ROOT_COMMAND + " enable <" + MODULE_LIST + ">"), ChatFormatting.YELLOW, false);
		append(message, Component.literal(ROOT_COMMAND + " disable <" + MODULE_LIST + ">"), ChatFormatting.YELLOW, false);

		append(message, Component.literal(""), ChatFormatting.WHITE, false);
		append(message, AdminMessages.t(source,
				"commands.teleport_commands.admin.help.section.config"), ChatFormatting.GOLD, true);
		append(message, Component.literal(ROOT_COMMAND + " config teleporting delay <seconds>"),
				ChatFormatting.YELLOW, false);
		append(message, Component.literal(ROOT_COMMAND + " config teleporting cooldown <seconds>"),
				ChatFormatting.YELLOW, false);
		append(message, Component.literal(ROOT_COMMAND + " config back deleteAfterTeleport <true|false>"),
				ChatFormatting.YELLOW, false);
		append(message, Component.literal(ROOT_COMMAND + " config home max <count>"), ChatFormatting.YELLOW, false);
		append(message, Component.literal(ROOT_COMMAND + " config home deleteInvalid <true|false>"),
				ChatFormatting.YELLOW, false);
		append(message, Component.literal(ROOT_COMMAND + " config tpa expireTime <seconds>"), ChatFormatting.YELLOW, false);
		append(message, Component.literal(ROOT_COMMAND + " config warp max <count>"), ChatFormatting.YELLOW, false);
		append(message, Component.literal(ROOT_COMMAND + " config warp deleteInvalid <true|false>"),
				ChatFormatting.YELLOW, false);
		append(message, Component.literal(ROOT_COMMAND + " config worldspawn world <worldId>"), ChatFormatting.YELLOW, false);
		append(message, Component.literal(ROOT_COMMAND + " config rtp radius <blocks>"), ChatFormatting.YELLOW, false);
		append(message, Component.literal(ROOT_COMMAND + " config xaero syncIntervalSeconds <seconds>"),
				ChatFormatting.YELLOW, false);

		return message;
	}

	private static void append(MutableComponent dst, MutableComponent line, ChatFormatting color, boolean bold) {
		MutableComponent text = line.copy().append("\n");
		dst.append(bold ? text.withStyle(color, ChatFormatting.BOLD) : text.withStyle(color));
	}
}
