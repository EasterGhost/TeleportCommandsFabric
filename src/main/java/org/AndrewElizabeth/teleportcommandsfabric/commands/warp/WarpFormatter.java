package org.AndrewElizabeth.teleportcommandsfabric.commands.warp;

import org.AndrewElizabeth.teleportcommandsfabric.commands.common.PaginationCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.commands.common.CommandUiSupport;
import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

final class WarpFormatter {
	private static final String WARP_LIST_COMMAND = "warps";
	private static final String WARP_PAGE_PICKER_COMMAND = "teleportcommandsfabric:warpspages";

	private WarpFormatter() {
	}

	static MutableComponent buildWarpList(CommandSourceStack source, ServerPlayer player, List<NamedLocation> warps,
			int currentPage, int totalPages) {
		MutableComponent message = Component.empty();
		message.append(PaginationCommandSupport.buildHeader(
				player,
				"commands.teleport_commands.warps.title",
				currentPage,
				totalPages));

		for (NamedLocation currentWarp : warps) {
			appendWarpEntry(message, source, player, currentWarp, currentPage);
		}

		message.append(PaginationCommandSupport.buildNavigation(
				player,
				currentPage,
				totalPages,
				WARP_LIST_COMMAND,
				WARP_PAGE_PICKER_COMMAND));
		return message;
	}

	static MutableComponent buildWarpPagePicker(ServerPlayer player, int currentPage, int totalPages) {
		return PaginationCommandSupport.buildPagePicker(
				player,
				"commands.teleport_commands.warps.title",
				currentPage,
				totalPages,
				WARP_LIST_COMMAND);
	}

	private static void appendWarpEntry(MutableComponent message, CommandSourceStack source, ServerPlayer player,
			NamedLocation currentWarp, int currentPage) {
		String quotedName = CommandHelper.quoteCommandArgument(currentWarp.getName());
		boolean canModify = source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
		boolean playerVisible = isVisibleForPlayer(player, currentWarp.getUuid());

		CommandUiSupport.appendNameLine(
				message,
				currentWarp.getName(),
				getTranslatedText(
						playerVisible
								? "commands.teleport_commands.common.mapVisible"
								: "commands.teleport_commands.common.mapHidden",
						player)
						.withStyle(playerVisible ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY));
		CommandUiSupport.appendLocationLine(message, player, currentWarp);

		message.append("\n");
		message.append(Component.literal("     | ").withStyle(ChatFormatting.AQUA))
				.append(CommandUiSupport.translatedButton(
						player,
						"commands.teleport_commands.common.tp",
						ChatFormatting.GREEN,
						new ClickEvent.RunCommand("warp " + quotedName)))
				.append(" ");

		if (canModify) {
			message.append(CommandUiSupport.translatedButton(
					player,
					"commands.teleport_commands.common.rename",
					ChatFormatting.BLUE,
					new ClickEvent.SuggestCommand("/renamewarp " + quotedName + " ")))
					.append(" ")
					.append(CommandUiSupport.translatedButton(
							player,
							"commands.teleport_commands.common.update",
							ChatFormatting.YELLOW,
							new ClickEvent.RunCommand("updatewarp " + quotedName)))
					.append(" ")
					.append(CommandUiSupport.translatedButton(
							player,
							"commands.teleport_commands.common.delete",
							ChatFormatting.RED,
							new ClickEvent.SuggestCommand("/delwarp " + quotedName)));
		}

		message.append(" ")
				.append(CommandUiSupport.translatedButton(
						player,
						playerVisible
								? "commands.teleport_commands.common.hideFromMap"
								: "commands.teleport_commands.common.showOnMap",
						playerVisible ? ChatFormatting.GRAY : ChatFormatting.GOLD,
						new ClickEvent.RunCommand(
								"teleportcommandsfabric:mapwarp " + quotedName + " "
										+ (playerVisible ? "false" : "true") + " " + currentPage)));

		message.append("\n");
	}

	private static boolean isVisibleForPlayer(ServerPlayer player, UUID warpUuid) {
		return org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager.STORAGE
				.getPlayer(player.getStringUUID())
				.map(playerData -> !playerData.isWarpHidden(warpUuid))
				.orElse(true);
	}
}
