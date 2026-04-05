package org.AndrewElizabeth.teleportcommandsfabric.core.command;

import org.AndrewElizabeth.teleportcommandsfabric.models.NamedLocation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

public final class CommandUiSupport {
	private CommandUiSupport() {
	}

	public static void appendNameLine(MutableComponent message, String name, MutableComponent... suffixes) {
		message.append("\n");
		message.append(Component.literal("  - " + name).withStyle(ChatFormatting.AQUA));
		for (MutableComponent suffix : suffixes) {
			if (suffix == null) {
				continue;
			}
			message.append(" ").append(suffix);
		}
	}

	public static void appendLocationLine(MutableComponent message, ServerPlayer player, NamedLocation location) {
		String coords = String.format("[X%d Y%d Z%d]", location.getX(), location.getY(), location.getZ());
		String dimension = String.format(" [%s]", location.getWorldString());

		message.append("\n");
		message.append(Component.literal("     | ").withStyle(ChatFormatting.AQUA))
				.append(Component.literal(coords)
						.withStyle(ChatFormatting.LIGHT_PURPLE)
						.withStyle(style -> style.withClickEvent(new ClickEvent.CopyToClipboard(
								String.format("X%d Y%d Z%d", location.getX(), location.getY(), location.getZ()))))
						.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(
								getTranslatedText("commands.teleport_commands.common.hoverCopy", player)))))
				.append(Component.literal(dimension)
						.withStyle(ChatFormatting.DARK_PURPLE)
						.withStyle(style -> style
								.withClickEvent(new ClickEvent.CopyToClipboard(location.getWorldString())))
						.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(
								getTranslatedText("commands.teleport_commands.common.hoverCopy", player)))));
	}

	public static MutableComponent translatedButton(ServerPlayer player, String key, ChatFormatting color,
			ClickEvent clickEvent) {
		MutableComponent component = getTranslatedText(key, player).withStyle(color);
		if (clickEvent != null) {
			component = component.withStyle(style -> style.withClickEvent(clickEvent));
		}
		return component;
	}
}
