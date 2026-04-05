package org.AndrewElizabeth.teleportcommandsfabric.modules.admin;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

final class AdminStatusFormatter {
	private static final String RUN_ROOT_COMMAND = "tpc";
	private static final String DISPLAY_ROOT_COMMAND = "/tpc";

	private AdminStatusFormatter() {
	}

	static MutableComponent build(CommandSourceStack source) {
		MutableComponent message = Component.empty();

		append(message, AdminMessages.t(source, "commands.teleport_commands.admin.stat.title"), ChatFormatting.GOLD, true);
		for (var entry : AdminModuleRegistry.entries()) {
			appendModuleStatus(message, source, entry.getKey(), entry.getValue());
		}

		return message;
	}

	static MutableComponent refreshDivider() {
		return Component.literal("\n===========================")
				.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
	}

	private static void appendModuleStatus(MutableComponent message, CommandSourceStack source, String moduleKey,
			AdminModuleRegistry.ModuleToggle toggle) {
		boolean enabled = toggle.enabled().getAsBoolean();
		String actionKey = enabled
				? "commands.teleport_commands.admin.action.disable"
				: "commands.teleport_commands.admin.action.enable";
		String runCommand = RUN_ROOT_COMMAND + " " + (enabled ? "disable " : "enable ") + moduleKey;
		String displayCommand = DISPLAY_ROOT_COMMAND + " " + (enabled ? "disable " : "enable ") + moduleKey;
		MutableComponent state = AdminMessages.t(source, enabled
				? "commands.teleport_commands.admin.stat.enabled"
				: "commands.teleport_commands.admin.stat.disabled")
				.withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
		MutableComponent action = Component.literal("[")
				.append(AdminMessages.t(source, actionKey))
				.append("]")
				.withStyle(style -> style
						.withColor(enabled ? ChatFormatting.RED : ChatFormatting.GREEN)
						.withClickEvent(new ClickEvent.RunCommand(runCommand))
						.withHoverEvent(new HoverEvent.ShowText(AdminMessages.t(source, "commands.teleport_commands.admin.action.hover",
								Component.literal(displayCommand)))));
		MutableComponent line = AdminMessages.t(source, "commands.teleport_commands.admin.stat.entry",
				AdminMessages.t(source, toggle.labelKey()), state)
				.append(Component.literal(" "))
				.append(action);
		append(message, line, ChatFormatting.WHITE, false);
	}

	private static void append(MutableComponent dst, MutableComponent line, ChatFormatting color, boolean bold) {
		MutableComponent text = line.copy().append("\n");
		dst.append(bold ? text.withStyle(color, ChatFormatting.BOLD) : text.withStyle(color));
	}
}
