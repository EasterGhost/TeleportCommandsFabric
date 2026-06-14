package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin;

import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.ComponentSupport;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Locale;

public final class AdminStatusRenderer {
	private static final String RUN_ROOT_COMMAND = "tpc";
	private static final String DISPLAY_ROOT_COMMAND = "/tpc";

	public Component render(List<AdminModuleStatus> modules, String language, AdminRuntimeInfo runtimeInfo) {
		String safeLanguage = normalizeLanguage(language);
		MutableComponent message = Component.empty();
		message.append(ComponentSupport.translate("commands.teleport_commands.admin.stat.header", safeLanguage)
				.withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
		AdminInfoRenderer.append(message, runtimeInfo, safeLanguage);
		message.append("\n");
		appendLine(message, ComponentSupport.translate("commands.teleport_commands.admin.stat.title", safeLanguage),
				ChatFormatting.GOLD, true);
		for (AdminModuleStatus module : safeModules(modules)) {
			appendLine(message, moduleStatusLine(module, safeLanguage), ChatFormatting.WHITE, false);
		}
		return message;
	}

	public Component renderRefreshDivider() {
		return Component.literal("\n===========================")
				.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
	}

	private MutableComponent moduleStatusLine(AdminModuleStatus module, String language) {
		boolean enabled = module.enabled();
		String actionName = enabled ? "disable" : "enable";
		String actionKey = enabled
				? "commands.teleport_commands.admin.action.disable"
				: "commands.teleport_commands.admin.action.enable";
		String runCommand = RUN_ROOT_COMMAND + " " + actionName + " " + module.moduleKey();
		String displayCommand = DISPLAY_ROOT_COMMAND + " " + actionName + " " + module.moduleKey();
		MutableComponent state = ComponentSupport.translate(enabled
				? "commands.teleport_commands.admin.stat.enabled"
				: "commands.teleport_commands.admin.stat.disabled", language)
				.withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
		MutableComponent action = Component.literal("[")
				.append(ComponentSupport.translate(actionKey, language))
				.append("]")
				.withStyle(style -> style
						.withColor(enabled ? ChatFormatting.RED : ChatFormatting.GREEN)
						.withClickEvent(new ClickEvent.RunCommand(runCommand))
						.withHoverEvent(new HoverEvent.ShowText(ComponentSupport.translate(
								"commands.teleport_commands.admin.action.hover", language,
								Component.literal(displayCommand)))));
		return ComponentSupport.translate("commands.teleport_commands.admin.stat.entry", language,
				ComponentSupport.translate(module.labelKey(), language), state)
				.append(Component.literal(" "))
				.append(action);
	}

	private void appendLine(MutableComponent dst, MutableComponent line, ChatFormatting color, boolean bold) {
		MutableComponent text = line.copy().append("\n");
		dst.append(bold ? text.withStyle(color, ChatFormatting.BOLD) : text.withStyle(color));
	}

	private List<AdminModuleStatus> safeModules(List<AdminModuleStatus> modules) {
		return modules == null ? List.of() : List.copyOf(modules);
	}

	private String normalizeLanguage(String language) {
		return language == null || language.isBlank() ? "en_us" : language.toLowerCase(Locale.ROOT);
	}
}
