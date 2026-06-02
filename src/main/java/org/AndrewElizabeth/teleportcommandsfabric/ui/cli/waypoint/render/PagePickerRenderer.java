package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.render;

import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.ComponentSupport;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Objects;

public final class PagePickerRenderer {
	private static final int PAGE_PICKER_COLUMNS = 8;

	private final CommandLinkBuilder commands;

	public PagePickerRenderer(CommandLinkBuilder commands) {
		this.commands = Objects.requireNonNull(commands, "commands");
	}

	public Component render(WaypointPageKind kind, WaypointListQuery query, int currentPage, int totalPages,
			String language) {
		WaypointPageKind safeKind = kind == null ? WaypointPageKind.HOMES : kind;
		WaypointListQuery safeQuery = query == null ? WaypointListQuery.defaultQuery() : query;
		int safeTotalPages = Math.max(1, totalPages);
		int safeCurrentPage = Math.min(Math.max(1, currentPage), safeTotalPages);
		String safeLanguage = language == null || language.isBlank() ? "en_us" : language.toLowerCase();

		MutableComponent picker = Component.empty();
		picker.append(ComponentSupport.translate("commands.teleport_commands.common.pagePickerTitle", safeLanguage,
				ComponentSupport.translate(ComponentSupport.waypointTitleKey(safeKind), safeLanguage),
				Component.literal(String.valueOf(safeCurrentPage)),
				Component.literal(String.valueOf(safeTotalPages))).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

		for (int page = 1; page <= safeTotalPages; page++) {
			picker.append((page - 1) % PAGE_PICKER_COLUMNS == 0 ? "\n" : " ");
			picker.append(ComponentSupport.pageButton(page, safeCurrentPage,
					commands.listCommand(safeKind, safeQuery, page)));
		}
		return picker;
	}
}
