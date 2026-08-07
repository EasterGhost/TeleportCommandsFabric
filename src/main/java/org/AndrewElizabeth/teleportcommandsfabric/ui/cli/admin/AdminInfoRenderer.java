package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin;

import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.ComponentSupport;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class AdminInfoRenderer {
	private AdminInfoRenderer() {
	}

	static void append(MutableComponent message, AdminRuntimeInfo info, String language) {
		AdminRuntimeInfo safeInfo = info == null ? AdminRuntimeInfo.versionOnly("unknown") : info;
		appendLine(message, ComponentSupport.translate("commands.teleport_commands.admin.info.version",
				language, Component.literal(safeInfo.version())).withStyle(ChatFormatting.GRAY));
		appendLine(message, ComponentSupport.translate("commands.teleport_commands.admin.info.integrations",
				language, integrations(safeInfo, language)).withStyle(ChatFormatting.GRAY));
	}

	private static MutableComponent integrations(AdminRuntimeInfo info, String language) {
		if (info.integrations().isEmpty()) {
			return ComponentSupport.translate("commands.teleport_commands.admin.info.integration.none", language)
					.withStyle(ChatFormatting.GRAY);
		}
		MutableComponent text = Component.empty();
		for (AdminIntegrationStatus integration : info.integrations()) {
			if (!text.getString().isEmpty()) {
				text.append(", ");
			}
			text.append(ComponentSupport.translate(integration.labelKey(), language).withStyle(ChatFormatting.AQUA));
			text.append(" ");
			text.append(ComponentSupport.translate(integration.stateKey(), language)
					.withStyle(ChatFormatting.GREEN));
		}
		return text;
	}

	private static void appendLine(MutableComponent message, MutableComponent line) {
		if (!message.getString().isEmpty()) {
			message.append("\n");
		}
		message.append(line);
	}
}
