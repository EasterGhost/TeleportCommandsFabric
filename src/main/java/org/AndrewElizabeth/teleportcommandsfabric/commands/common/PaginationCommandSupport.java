package org.AndrewElizabeth.teleportcommandsfabric.commands.common;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.function.IntFunction;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

public final class PaginationCommandSupport {
	public static final int PAGE_SIZE = 4;
	private static final int PAGE_PICKER_COLUMNS = 8;
	private static final int NAVIGATION_PAGE_RADIUS = 2;

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
			IntFunction<String> listCommandFactory, IntFunction<String> jumpCommandFactory) {
		MutableComponent navigation = Component.empty();
		navigation.append("\n");
		navigation.append(buildNavButton(player, "commands.teleport_commands.common.first",
				currentPage > 1 ? listCommandFactory.apply(1) : null));
		navigation.append(" ");
		navigation.append(buildNavButton(player, "commands.teleport_commands.common.prev",
				currentPage > 1 ? listCommandFactory.apply(currentPage - 1) : null));
		navigation.append(" ");

		int startPage = Math.max(1, currentPage - NAVIGATION_PAGE_RADIUS);
		int endPage = Math.min(totalPages, currentPage + NAVIGATION_PAGE_RADIUS);
		if (startPage == 1) {
			endPage = Math.min(totalPages, startPage + (NAVIGATION_PAGE_RADIUS * 2));
		}
		if (endPage == totalPages) {
			startPage = Math.max(1, endPage - (NAVIGATION_PAGE_RADIUS * 2));
		}
		for (int page = startPage; page <= endPage; page++) {
			if (page > startPage) {
				navigation.append(" ");
			}
			navigation.append(buildPageButton(page, currentPage, listCommandFactory.apply(page)));
		}

		navigation.append(" ");
		navigation.append(buildNavButton(player, "commands.teleport_commands.common.jump",
				jumpCommandFactory.apply(currentPage)));
		navigation.append(" ");
		navigation.append(buildNavButton(player, "commands.teleport_commands.common.next",
				currentPage < totalPages ? listCommandFactory.apply(currentPage + 1) : null));
		navigation.append(" ");
		navigation.append(buildNavButton(player, "commands.teleport_commands.common.last",
				currentPage < totalPages ? listCommandFactory.apply(totalPages) : null));
		return navigation;
	}

	public static MutableComponent buildPagePicker(ServerPlayer player, String titleKey, int currentPage,
			int totalPages,
			IntFunction<String> listCommandFactory) {
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
			picker.append(buildPageButton(page, currentPage, listCommandFactory.apply(page)));
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

	private static MutableComponent buildPageButton(int page, int currentPage, String command) {
		MutableComponent button = Component.literal("[" + page + "]");
		if (page == currentPage) {
			return button.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
		}
		return button.withStyle(ChatFormatting.GREEN)
				.withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(command)));
	}
}
