package org.AndrewElizabeth.teleportcommandsfabric.ui.cli;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.PageView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.WaypointPageKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.WaypointPageRequest;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;
import java.util.Objects;

public final class WaypointPageRenderer {
	private static final int NAVIGATION_PAGE_RADIUS = 2;

	private final WaypointCommandFactory commands;

	public WaypointPageRenderer(WaypointCommandFactory commands) {
		this.commands = Objects.requireNonNull(commands, "commands");
	}

	public Component render(WaypointPageRequest request, PageView<NamedLocationView> page) {
		MutableComponent message = Component.empty();
		message.append(buildHeader(request, page));
		for (NamedLocationView location : page.entries()) {
			appendEntry(message, request, location, page.currentPage());
		}
		message.append(buildNavigation(request, page.currentPage(), page.totalPages()));
		return message;
	}

	private MutableComponent buildHeader(WaypointPageRequest request, PageView<NamedLocationView> page) {
		return ComponentSupport.waypointHeader(request.kind(), page.currentPage(), page.totalPages(), request.language());
	}

	private void appendEntry(MutableComponent message, WaypointPageRequest request, NamedLocationView location, int currentPage) {
		String quotedName = CommandArgumentUtils.quote(location.getName());
		boolean mapVisible = isMapVisible(request, location);

		message.append("\n");
		message.append(Component.literal("  - " + location.getName()).withStyle(ChatFormatting.AQUA));
		appendMarkers(message, request, location, quotedName, mapVisible);
		appendLocationLine(message, request.language(), location);
		appendActionLine(message, request, location, quotedName, mapVisible, currentPage);
		message.append("\n");
	}

	private void appendMarkers(MutableComponent message, WaypointPageRequest request, NamedLocationView location,
			String quotedName, boolean mapVisible) {
		if (request.kind() == WaypointPageKind.HOMES && Objects.equals(request.defaultLocationUuid(), location.getUuid())) {
			appendMarker(message, request.language(), "commands.teleport_commands.common.default",
					ChatFormatting.AQUA, ChatFormatting.BOLD);
		}
		if (request.kind() == WaypointPageKind.HOMES && location.isTemporary()) {
			appendMarker(message, request.language(), "commands.teleport_commands.home.temporary",
					ChatFormatting.GOLD, ChatFormatting.BOLD);
		}
		appendMarker(message, request.language(), mapVisible
				? "commands.teleport_commands.common.mapVisible"
				: "commands.teleport_commands.common.mapHidden", mapVisible ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY);
		if (request.kind() == WaypointPageKind.WARPS && request.canModify()) {
			appendClickableMarker(message, request.language(), location.isVisible()
					? "commands.teleport_commands.gwarpmap.globalVisible"
					: "commands.teleport_commands.gwarpmap.globalHidden",
					location.isVisible() ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY, ChatFormatting.UNDERLINE,
					new ClickEvent.RunCommand(commands.globalWarpVisibilityCommand(quotedName, !location.isVisible())));
		}
	}

	private void appendLocationLine(MutableComponent message, String language, NamedLocationView location) {
		String coords = String.format(Locale.ROOT, "[X%d Y%d Z%d]", location.getX(), location.getY(), location.getZ());
		String copyCoords = String.format(Locale.ROOT, "X%d Y%d Z%d", location.getX(), location.getY(), location.getZ());
		String dimension = " [" + location.getDimensionId() + "]";

		message.append("\n");
		message.append(Component.literal("     | ").withStyle(ChatFormatting.AQUA))
				.append(copyableText(coords, copyCoords, ChatFormatting.LIGHT_PURPLE, language))
				.append(copyableText(dimension, location.getDimensionId(), ChatFormatting.DARK_PURPLE, language));
	}

	private void appendActionLine(MutableComponent message, WaypointPageRequest request, NamedLocationView location,
			String quotedName, boolean mapVisible, int currentPage) {
		message.append("\n");
		message.append(Component.literal("     | ").withStyle(ChatFormatting.AQUA));
		appendActionButton(message, request.language(), "commands.teleport_commands.common.tp", ChatFormatting.GREEN,
				new ClickEvent.RunCommand(commands.teleportCommand(request.kind(), quotedName)));

		if (request.canModify()) {
			appendModifyButtons(message, request, location, quotedName);
		}

		appendActionButton(message, request.language(), mapVisible
				? "commands.teleport_commands.common.hideFromMap"
				: "commands.teleport_commands.common.showOnMap", mapVisible ? ChatFormatting.GRAY : ChatFormatting.GOLD,
				new ClickEvent.RunCommand(commands.visibilityCommand(request, quotedName, !mapVisible, currentPage)));
	}

	private void appendModifyButtons(MutableComponent message, WaypointPageRequest request, NamedLocationView location, String quotedName) {
		appendActionButton(message, request.language(), "commands.teleport_commands.common.rename", ChatFormatting.BLUE,
				new ClickEvent.SuggestCommand("/" + commands.renameCommand(request.kind()) + " " + quotedName + " "));
		appendActionButton(message, request.language(), "commands.teleport_commands.common.update", ChatFormatting.YELLOW,
				new ClickEvent.RunCommand(commands.updateCommand(request.kind()) + " " + quotedName));
		if (request.kind() == WaypointPageKind.HOMES && !location.isTemporary()
				&& !Objects.equals(request.defaultLocationUuid(), location.getUuid())) {
			appendActionButton(message, request.language(), "commands.teleport_commands.common.defaultPrompt", ChatFormatting.DARK_AQUA,
					new ClickEvent.RunCommand("defaulthome " + quotedName));
		}
		appendActionButton(message, request.language(), "commands.teleport_commands.common.delete", ChatFormatting.RED,
				new ClickEvent.SuggestCommand("/" + commands.deleteCommand(request.kind()) + " " + quotedName));
	}

	private MutableComponent buildNavigation(WaypointPageRequest request, int currentPage, int totalPages) {
		MutableComponent navigation = Component.empty();
		navigation.append("\n");
		navigation.append(ComponentSupport.navButton(request.language(), "commands.teleport_commands.common.first",
				currentPage > 1 ? commands.listCommand(request.kind(), request.query(), 1) : null));
		navigation.append(" ");
		navigation.append(ComponentSupport.navButton(request.language(), "commands.teleport_commands.common.prev",
				currentPage > 1 ? commands.listCommand(request.kind(), request.query(), currentPage - 1) : null));
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
			navigation.append(ComponentSupport.pageButton(page, currentPage,
					commands.listCommand(request.kind(), request.query(), page)));
		}

		navigation.append(" ");
		navigation.append(ComponentSupport.navButton(request.language(), "commands.teleport_commands.common.jump",
				commands.pagePickerCommand(request.kind(), request.query(), currentPage)));
		navigation.append(" ");
		navigation.append(ComponentSupport.navButton(request.language(), "commands.teleport_commands.common.next",
				currentPage < totalPages ? commands.listCommand(request.kind(), request.query(), currentPage + 1) : null));
		navigation.append(" ");
		navigation.append(ComponentSupport.navButton(request.language(), "commands.teleport_commands.common.last",
				currentPage < totalPages ? commands.listCommand(request.kind(), request.query(), totalPages) : null));
		return navigation;
	}

	private MutableComponent button(String language, String key, ChatFormatting color, ClickEvent clickEvent) {
		return ComponentSupport.translatedButton(language, key, color, clickEvent);
	}

	private void appendActionButton(MutableComponent message, String language, String key, ChatFormatting color, ClickEvent clickEvent) {
		message.append(button(language, key, color, clickEvent));
		message.append(" ");
	}

	private void appendMarker(MutableComponent message, String language, String key, ChatFormatting... formatting) {
		message.append(" ");
		message.append(ComponentSupport.translate(key, language).withStyle(formatting));
	}

	private void appendClickableMarker(MutableComponent message, String language, String key, ChatFormatting color, ChatFormatting decoration,
		ClickEvent clickEvent) {
		message.append(" ");
		message.append(ComponentSupport.translate(key, language).withStyle(color, decoration)
				.withStyle(style -> style.withClickEvent(clickEvent)));
	}

	private MutableComponent copyableText(String text, String copyValue, ChatFormatting color, String language) {
		return Component.literal(text)
				.withStyle(color)
				.withStyle(style -> style.withClickEvent(new ClickEvent.CopyToClipboard(copyValue)))
				.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(
						ComponentSupport.translate("commands.teleport_commands.common.hoverCopy", language))));
	}

	private boolean isMapVisible(WaypointPageRequest request, NamedLocationView location) {
		if (request.kind() == WaypointPageKind.WARPS) {
			return !request.hiddenWarpUuids().contains(location.getUuid());
		}
		return location.isVisible();
	}

}
