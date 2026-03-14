package org.AndrewElizabeth.teleportcommandsfabric.commands.warp;

import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.CommandHelper.quoteCommandArgument;
import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

final class WarpAdminFormatter {
	private WarpAdminFormatter() {
	}

	static MutableComponent buildWarpMapList(ServerPlayer player, List<NamedLocation> warps) {
		MutableComponent message = Component.empty();
		message.append(getTranslatedText("commands.teleport_commands.gwarpmap.warps", player)
				.withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

		for (NamedLocation currentWarp : warps) {
			appendWarpEntry(message, player, currentWarp);
		}

		return message;
	}

	private static void appendWarpEntry(MutableComponent message, ServerPlayer player, NamedLocation currentWarp) {
		String name = String.format("  - %s", currentWarp.getName());
		String quotedName = quoteCommandArgument(currentWarp.getName());
		String coords = String.format("[X%d Y%d Z%d]", currentWarp.getX(), currentWarp.getY(), currentWarp.getZ());
		String dimension = String.format(" [%s]", currentWarp.getWorldString());
		boolean visible = currentWarp.isXaeroVisible();
		MutableComponent toggleButton = getTranslatedText(
				visible
						? "commands.teleport_commands.gwarpmap.hideFromMap"
						: "commands.teleport_commands.gwarpmap.showOnMap",
				player)
				.withStyle(visible ? ChatFormatting.GRAY : ChatFormatting.GOLD)
				.withStyle(style -> style.withClickEvent(
						new ClickEvent.RunCommand("gwarpmap " + quotedName + " " + (visible ? "false" : "true"))));

		message.append("\n");
		message.append(Component.literal(name).withStyle(ChatFormatting.AQUA));
		message.append(" ")
				.append(getTranslatedText(
						visible
								? "commands.teleport_commands.gwarpmap.globalVisible"
								: "commands.teleport_commands.gwarpmap.globalHidden",
						player)
						.withStyle(visible ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY));
		message.append(" ").append(toggleButton);

		message.append("\n");
		message.append(Component.literal("     | ")
				.withStyle(ChatFormatting.AQUA))
				.append(Component.literal(coords)
						.withStyle(ChatFormatting.LIGHT_PURPLE)
						.withStyle(style -> style.withClickEvent(
								new ClickEvent.CopyToClipboard(
										String.format("X%d Y%d Z%d", currentWarp.getX(), currentWarp.getY(),
												currentWarp.getZ()))))
						.withStyle(style -> style.withHoverEvent(
								new HoverEvent.ShowText(
										getTranslatedText("commands.teleport_commands.common.hoverCopy", player)))))
				.append(Component.literal(dimension)
						.withStyle(ChatFormatting.DARK_PURPLE)
						.withStyle(style -> style.withClickEvent(new ClickEvent.CopyToClipboard(currentWarp.getWorldString())))
						.withStyle(style -> style.withHoverEvent(
								new HoverEvent.ShowText(
										getTranslatedText("commands.teleport_commands.common.hoverCopy", player)))));

		message.append("\n");
	}
}
