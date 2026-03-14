package org.AndrewElizabeth.teleportcommandsfabric.commands.home;

import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.common.Player;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.CommandHelper.quoteCommandArgument;
import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

final class HomeFormatter {
	private HomeFormatter() {
	}

	static MutableComponent buildHomeList(ServerPlayer player, Player playerStorage, List<NamedLocation> homes) {
		MutableComponent message = Component.empty();
		message.append(getTranslatedText("commands.teleport_commands.homes.homes", player)
				.withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

		for (NamedLocation currentHome : homes) {
			appendHomeEntry(message, player, playerStorage, currentHome);
		}

		return message;
	}

	private static void appendHomeEntry(MutableComponent message, ServerPlayer player, Player playerStorage,
			NamedLocation currentHome) {
		String name = String.format("  - %s", currentHome.getName());
		String quotedName = quoteCommandArgument(currentHome.getName());
		String coords = String.format("[X%d Y%d Z%d]", currentHome.getX(), currentHome.getY(), currentHome.getZ());
		String dimension = String.format(" [%s]", currentHome.getWorldString());

		message.append("\n");
		message.append(Component.literal(name).withStyle(ChatFormatting.AQUA));

		if (currentHome.getUuid().equals(playerStorage.getDefaultHomeUuid())) {
			message.append(" ")
					.append(getTranslatedText("commands.teleport_commands.common.default", player)
							.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
		}

		message.append(" ")
				.append(getTranslatedText(
						currentHome.isXaeroVisible()
								? "commands.teleport_commands.common.mapVisible"
								: "commands.teleport_commands.common.mapHidden",
						player)
						.withStyle(currentHome.isXaeroVisible() ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY));

		message.append("\n");
		message.append(Component.literal("     | ")
				.withStyle(ChatFormatting.AQUA))
				.append(Component.literal(coords)
						.withStyle(ChatFormatting.LIGHT_PURPLE)
						.withStyle(style -> style.withClickEvent(
								new ClickEvent.CopyToClipboard(
										String.format("X%d Y%d Z%d", currentHome.getX(), currentHome.getY(),
												currentHome.getZ()))))
						.withStyle(style -> style.withHoverEvent(
								new HoverEvent.ShowText(
										getTranslatedText("commands.teleport_commands.common.hoverCopy", player)))))
				.append(Component.literal(dimension)
						.withStyle(ChatFormatting.DARK_PURPLE)
						.withStyle(style -> style
								.withClickEvent(new ClickEvent.CopyToClipboard(currentHome.getWorldString())))
						.withStyle(style -> style.withHoverEvent(
								new HoverEvent.ShowText(
										getTranslatedText("commands.teleport_commands.common.hoverCopy", player)))));

		message.append("\n");
		message.append(Component.literal("     | ")
				.withStyle(ChatFormatting.AQUA))
				.append(getTranslatedText("commands.teleport_commands.common.tp", player)
						.withStyle(ChatFormatting.GREEN)
						.withStyle(style -> style.withClickEvent(
								new ClickEvent.RunCommand("home " + quotedName))))
				.append(" ")
				.append(getTranslatedText("commands.teleport_commands.common.rename", player)
						.withStyle(ChatFormatting.BLUE)
						.withStyle(style -> style.withClickEvent(
								new ClickEvent.SuggestCommand("/renamehome " + quotedName + " "))))
				.append(" ");

		if (!currentHome.getUuid().equals(playerStorage.getDefaultHomeUuid())) {
			message.append(getTranslatedText("commands.teleport_commands.common.defaultPrompt", player)
					.withStyle(ChatFormatting.DARK_AQUA)
					.withStyle(style -> style.withClickEvent(
							new ClickEvent.RunCommand("defaulthome " + quotedName))))
					.append(" ");
		}

		message.append(getTranslatedText("commands.teleport_commands.common.delete", player)
				.withStyle(ChatFormatting.RED)
				.withStyle(style -> style.withClickEvent(
						new ClickEvent.SuggestCommand("/delhome " + quotedName))))
				.append(" ")
				.append(getTranslatedText(
						currentHome.isXaeroVisible()
								? "commands.teleport_commands.common.hideFromMap"
								: "commands.teleport_commands.common.showOnMap",
						player)
						.withStyle(currentHome.isXaeroVisible() ? ChatFormatting.GRAY : ChatFormatting.GOLD)
						.withStyle(style -> style.withClickEvent(
								new ClickEvent.RunCommand(
										"maphome " + quotedName + " "
												+ (currentHome.isXaeroVisible() ? "false" : "true")))));

		message.append("\n");
	}
}
