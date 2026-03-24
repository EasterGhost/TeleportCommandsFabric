package org.AndrewElizabeth.teleportcommandsfabric.commands.common;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

public final class PaginationCommandSupport {
	public static final int PAGE_SIZE = 4;
	private static final int PAGE_PICKER_COLUMNS = 8;

	private PaginationCommandSupport() {
	}

	public static int getTotalPages(int totalItems) {
		return Math.max(1, (totalItems + PAGE_SIZE - 1) / PAGE_SIZE);
	}

	public static boolean isValidPage(int page, int totalPages) {
		return page >= 1 && page <= totalPages;
	}

	public static <T> List<T> getPageEntries(List<T> entries, int page) {
		int fromIndex = (page - 1) * PAGE_SIZE;
		int toIndex = Math.min(fromIndex + PAGE_SIZE, entries.size());
		return entries.subList(fromIndex, toIndex);
	}

	public static MutableComponent buildHeader(ServerPlayer player, String titleKey, int currentPage, int totalPages) {
		MutableComponent header = Component.literal("========== ").withStyle(ChatFormatting.DARK_GRAY);
		header.append(getTranslatedText(titleKey, player).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
		header.append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY));
		header.append(getTranslatedText(
				"commands.teleport_commands.common.page",
				player,
				Component.literal(String.valueOf(currentPage)),
				Component.literal(String.valueOf(totalPages))).withStyle(ChatFormatting.GOLD));
		header.append(Component.literal(") ==========").withStyle(ChatFormatting.DARK_GRAY));
		return header;
	}

	public static MutableComponent buildNavigation(ServerPlayer player, int currentPage, int totalPages,
			String listCommand, String jumpCommand) {
		MutableComponent navigation = Component.empty();
		navigation.append("\n");
		navigation.append(buildNavButton(player, "commands.teleport_commands.common.first",
				currentPage > 1 ? listCommand + " 1" : null));
		navigation.append(" ");
		navigation.append(buildNavButton(player, "commands.teleport_commands.common.prev",
				currentPage > 1 ? listCommand + " " + (currentPage - 1) : null));
		navigation.append(" ");

		int startPage = Math.max(1, currentPage - 1);
		int endPage = Math.min(totalPages, startPage + 2);
		startPage = Math.max(1, endPage - 2);
		for (int page = startPage; page <= endPage; page++) {
			if (page > startPage) {
				navigation.append(" ");
			}
			navigation.append(buildPageButton(page, currentPage, listCommand));
		}

		navigation.append(" ");
		navigation.append(buildNavButton(player, "commands.teleport_commands.common.jump",
				jumpCommand + " " + currentPage));
		navigation.append(" ");
		navigation.append(buildNavButton(player, "commands.teleport_commands.common.next",
				currentPage < totalPages ? listCommand + " " + (currentPage + 1) : null));
		navigation.append(" ");
		navigation.append(buildNavButton(player, "commands.teleport_commands.common.last",
				currentPage < totalPages ? listCommand + " " + totalPages : null));
		return navigation;
	}

	public static MutableComponent buildPagePicker(ServerPlayer player, String titleKey, int currentPage, int totalPages,
			String listCommand) {
		MutableComponent picker = Component.empty();
		picker.append(getTranslatedText(
				"commands.teleport_commands.common.pagePickerTitle",
				player,
				getTranslatedText(titleKey, player),
				Component.literal(String.valueOf(currentPage)),
				Component.literal(String.valueOf(totalPages)))
				.withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

		for (int page = 1; page <= totalPages; page++) {
			if ((page - 1) % PAGE_PICKER_COLUMNS == 0) {
				picker.append("\n");
			} else {
				picker.append(" ");
			}
			picker.append(buildPageButton(page, currentPage, listCommand));
		}

		return picker;
	}

	private static MutableComponent buildNavButton(ServerPlayer player, String translationKey, String command) {
		MutableComponent button = getTranslatedText(translationKey, player);
		if (command == null) {
			return button.withStyle(ChatFormatting.DARK_GRAY);
		}
		return button.withStyle(ChatFormatting.AQUA)
				.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(command)));
	}

	private static MutableComponent buildPageButton(int page, int currentPage, String listCommand) {
		MutableComponent button = Component.literal("[" + page + "]");
		if (page == currentPage) {
			return button.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
		}
		return button.withStyle(ChatFormatting.GREEN)
				.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(listCommand + " " + page)));
	}
}
