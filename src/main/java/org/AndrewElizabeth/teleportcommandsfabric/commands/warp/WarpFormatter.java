package org.AndrewElizabeth.teleportcommandsfabric.commands.warp;

import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.CommandHelper.quoteCommandArgument;
import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

final class WarpFormatter {
	private WarpFormatter() {
	}

	static MutableComponent buildWarpList(CommandSourceStack source, ServerPlayer player, List<NamedLocation> warps) {
		MutableComponent message = Component.empty();
		message.append(getTranslatedText("commands.teleport_commands.warps.warps", player)
				.withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

		for (NamedLocation currentWarp : warps) {
			appendWarpEntry(message, source, player, currentWarp);
		}

		return message;
	}

	private static void appendWarpEntry(MutableComponent message, CommandSourceStack source, ServerPlayer player,
			NamedLocation currentWarp) {
		String name = String.format("  - %s", currentWarp.getName());
		String quotedName = quoteCommandArgument(currentWarp.getName());
		String coords = String.format("[X%d Y%d Z%d]", currentWarp.getX(), currentWarp.getY(), currentWarp.getZ());
		String dimension = String.format(" [%s]", currentWarp.getWorldString());
		boolean canModify = source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
		boolean playerVisible = isVisibleForPlayer(player, currentWarp.getUuid());

		message.append("\n");
		message.append(Component.literal(name).withStyle(ChatFormatting.AQUA));
		message.append(" ")
				.append(getTranslatedText(
						playerVisible
								? "commands.teleport_commands.common.mapVisible"
								: "commands.teleport_commands.common.mapHidden",
						player)
						.withStyle(playerVisible ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY));

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
						.withStyle(style -> style
								.withClickEvent(new ClickEvent.CopyToClipboard(currentWarp.getWorldString())))
						.withStyle(style -> style.withHoverEvent(
								new HoverEvent.ShowText(
										getTranslatedText("commands.teleport_commands.common.hoverCopy", player)))));

		message.append("\n");
		message.append(Component.literal("     | ").withStyle(ChatFormatting.AQUA))
				.append(getTranslatedText("commands.teleport_commands.common.tp", player)
						.withStyle(ChatFormatting.GREEN)
						.withStyle(style -> style.withClickEvent(
								new ClickEvent.RunCommand("warp " + quotedName))))
				.append(" ");

		if (canModify) {
			message.append(getTranslatedText("commands.teleport_commands.common.rename", player)
					.withStyle(ChatFormatting.BLUE)
					.withStyle(style -> style.withClickEvent(
							new ClickEvent.SuggestCommand("/renamewarp " + quotedName + " "))))
					.append(" ")
					.append(getTranslatedText("commands.teleport_commands.common.delete", player)
							.withStyle(ChatFormatting.RED)
							.withStyle(style -> style.withClickEvent(
									new ClickEvent.SuggestCommand("/delwarp " + quotedName))));
		}

		message.append(" ")
				.append(getTranslatedText(
						playerVisible
								? "commands.teleport_commands.common.hideFromMap"
								: "commands.teleport_commands.common.showOnMap",
						player)
						.withStyle(playerVisible ? ChatFormatting.GRAY : ChatFormatting.GOLD)
						.withStyle(style -> style.withClickEvent(
								new ClickEvent.RunCommand(
										"mapwarp " + quotedName + " " + (playerVisible ? "false" : "true")))));

		message.append("\n");
	}

	private static boolean isVisibleForPlayer(ServerPlayer player, UUID warpUuid) {
		return org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager.STORAGE
				.getPlayer(player.getStringUUID())
				.map(playerData -> !playerData.isWarpHidden(warpUuid))
				.orElse(true);
	}
}
