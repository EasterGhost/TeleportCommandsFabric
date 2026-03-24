package org.AndrewElizabeth.teleportcommandsfabric.commands.home;

import org.AndrewElizabeth.teleportcommandsfabric.commands.common.PaginationCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.commands.common.CommandUiSupport;
import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.common.Player;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

final class HomeFormatter {
	private static final String HOME_LIST_COMMAND = "homes";
	private static final String HOME_PAGE_PICKER_COMMAND = "teleportcommandsfabric:homespages";

	private HomeFormatter() {
	}

	static MutableComponent buildHomeList(ServerPlayer player, Player playerStorage, List<NamedLocation> homes,
			int currentPage, int totalPages) {
		MutableComponent message = Component.empty();
		message.append(PaginationCommandSupport.buildHeader(
				player,
				"commands.teleport_commands.homes.title",
				currentPage,
				totalPages));

		for (NamedLocation currentHome : homes) {
			appendHomeEntry(message, player, playerStorage, currentHome, currentPage);
		}

		message.append(PaginationCommandSupport.buildNavigation(
				player,
				currentPage,
				totalPages,
				HOME_LIST_COMMAND,
				HOME_PAGE_PICKER_COMMAND));
		return message;
	}

	static MutableComponent buildHomePagePicker(ServerPlayer player, int currentPage, int totalPages) {
		return PaginationCommandSupport.buildPagePicker(
				player,
				"commands.teleport_commands.homes.title",
				currentPage,
				totalPages,
				HOME_LIST_COMMAND);
	}

	private static void appendHomeEntry(MutableComponent message, ServerPlayer player, Player playerStorage,
			NamedLocation currentHome, int currentPage) {
		String quotedName = CommandHelper.quoteCommandArgument(currentHome.getName());
		MutableComponent defaultMarker = currentHome.getUuid().equals(playerStorage.getDefaultHomeUuid())
				? getTranslatedText("commands.teleport_commands.common.default", player)
						.withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
				: null;
		MutableComponent mapState = getTranslatedText(
				currentHome.isXaeroVisible()
						? "commands.teleport_commands.common.mapVisible"
						: "commands.teleport_commands.common.mapHidden",
				player)
				.withStyle(currentHome.isXaeroVisible() ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY);

		CommandUiSupport.appendNameLine(message, currentHome.getName(), defaultMarker, mapState);
		CommandUiSupport.appendLocationLine(message, player, currentHome);

		message.append("\n");
		message.append(Component.literal("     | ").withStyle(ChatFormatting.AQUA))
				.append(CommandUiSupport.translatedButton(
						player,
						"commands.teleport_commands.common.tp",
						ChatFormatting.GREEN,
						new ClickEvent.RunCommand("home " + quotedName)))
				.append(" ")
				.append(CommandUiSupport.translatedButton(
						player,
						"commands.teleport_commands.common.rename",
						ChatFormatting.BLUE,
						new ClickEvent.SuggestCommand("/renamehome " + quotedName + " ")))
				.append(" ");

		if (!currentHome.getUuid().equals(playerStorage.getDefaultHomeUuid())) {
			message.append(CommandUiSupport.translatedButton(
					player,
					"commands.teleport_commands.common.defaultPrompt",
					ChatFormatting.DARK_AQUA,
					new ClickEvent.RunCommand("defaulthome " + quotedName)))
					.append(" ");
		}

		message.append(CommandUiSupport.translatedButton(
				player,
				"commands.teleport_commands.common.delete",
				ChatFormatting.RED,
				new ClickEvent.SuggestCommand("/delhome " + quotedName)))
				.append(" ")
				.append(CommandUiSupport.translatedButton(
						player,
						currentHome.isXaeroVisible()
								? "commands.teleport_commands.common.hideFromMap"
								: "commands.teleport_commands.common.showOnMap",
						currentHome.isXaeroVisible() ? ChatFormatting.GRAY : ChatFormatting.GOLD,
						new ClickEvent.RunCommand(
								"teleportcommandsfabric:maphome " + quotedName + " "
										+ (currentHome.isXaeroVisible() ? "false" : "true") + " "
										+ currentPage)));

		message.append("\n");
	}
}
