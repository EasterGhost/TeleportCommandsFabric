package org.AndrewElizabeth.teleportcommandsfabric.commands.warp;

import org.AndrewElizabeth.teleportcommandsfabric.commands.common.CommandUiSupport;
import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

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
		String quotedName = CommandHelper.quoteCommandArgument(currentWarp.getName());
		boolean visible = currentWarp.isXaeroVisible();
		MutableComponent visibilityState = getTranslatedText(
				visible
						? "commands.teleport_commands.gwarpmap.globalVisible"
						: "commands.teleport_commands.gwarpmap.globalHidden",
				player)
				.withStyle(visible ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY);
		MutableComponent toggleButton = CommandUiSupport.translatedButton(
				player,
				visible
						? "commands.teleport_commands.gwarpmap.hideFromMap"
						: "commands.teleport_commands.gwarpmap.showOnMap",
				visible ? ChatFormatting.GRAY : ChatFormatting.GOLD,
				new ClickEvent.RunCommand("gwarpmap " + quotedName + " " + (visible ? "false" : "true")));
		MutableComponent resetButton = CommandUiSupport.translatedButton(
				player,
				"commands.teleport_commands.common.update",
				ChatFormatting.YELLOW,
				new ClickEvent.RunCommand("updatewarp " + quotedName));

		CommandUiSupport.appendNameLine(message, currentWarp.getName(), visibilityState, toggleButton, resetButton);
		CommandUiSupport.appendLocationLine(message, player, currentWarp);

		message.append("\n");
	}
}
