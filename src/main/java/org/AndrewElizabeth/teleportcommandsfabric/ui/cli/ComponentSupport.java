package org.AndrewElizabeth.teleportcommandsfabric.ui.cli;

import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.WaypointPageKind;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

final class ComponentSupport {
	private ComponentSupport() {
	}

	static MutableComponent waypointHeader(WaypointPageKind kind, int currentPage, int totalPages, String language) {
		MutableComponent header = Component.literal("========== ").withStyle(ChatFormatting.DARK_GRAY);
		header.append(translate(waypointTitleKey(kind), language).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
		header.append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY));
		header.append(translate("commands.teleport_commands.common.page", language,
				Component.literal(String.valueOf(currentPage)),
				Component.literal(String.valueOf(totalPages))).withStyle(ChatFormatting.GOLD));
		header.append(Component.literal(") ==========").withStyle(ChatFormatting.DARK_GRAY));
		return header;
	}

	static MutableComponent navButton(String language, String translationKey, String command) {
		MutableComponent button = translate(translationKey, language);
		if (command == null) {
			return button.withStyle(ChatFormatting.DARK_GRAY);
		}
		return button.withStyle(ChatFormatting.AQUA)
				.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(command)));
	}

	static MutableComponent pageButton(int page, int currentPage, String command) {
		MutableComponent button = Component.literal("[" + page + "]");
		if (page == currentPage) {
			return button.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
		}
		return button.withStyle(ChatFormatting.GREEN)
				.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(command)));
	}

	static MutableComponent translatedButton(String language, String key, ChatFormatting color, ClickEvent clickEvent) {
		MutableComponent component = translate(key, language).withStyle(color);
		if (clickEvent == null) {
			return component;
		}
		return component.withStyle(style -> style.withClickEvent(clickEvent));
	}

	static MutableComponent translate(String key, String language, MutableComponent... args) {
		return TranslationHelper.getTranslatedText(key, language, args);
	}

	static String waypointTitleKey(WaypointPageKind kind) {
		return kind == WaypointPageKind.HOMES
				? "commands.teleport_commands.homes.title"
				: "commands.teleport_commands.warps.title";
	}
}
