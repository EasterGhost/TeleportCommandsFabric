package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.render;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.ComponentSupport;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageRequest;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;
import java.util.Objects;

public final class ManageRenderer {
	private final CommandLinkBuilder commands;

	public ManageRenderer(CommandLinkBuilder commands) {
		this.commands = Objects.requireNonNull(commands, "commands");
	}

	public Component render(WaypointPageRequest request, NamedLocationView location) {
		MutableComponent message = header(request);
		appendSummary(message, request, location);
		appendActions(message, request, location);
		return message;
	}

	public Component renderDeleteConfirmation(WaypointPageRequest request, NamedLocationView location) {
		MutableComponent message = header(request);
		message.append("\n");
		message.append(ComponentSupport.translate("commands.teleport_commands.common.deletePrompt", request.language(),
				Component.literal(location.getName())).withStyle(ChatFormatting.RED));
		message.append("\n");
		message.append(button(request.language(), "commands.teleport_commands.common.confirmDelete", ChatFormatting.RED,
				commands.confirmDeleteCommand(request, location.getUuid(), request.query().page())));
		message.append(" ");
		message.append(button(request.language(), "commands.teleport_commands.common.cancel", ChatFormatting.AQUA,
				commands.manageCommand(request, location.getUuid(), request.query().page())));
		return message;
	}

	private MutableComponent header(WaypointPageRequest request) {
		MutableComponent header = Component.literal("========== ").withStyle(ChatFormatting.DARK_GRAY);
		header.append(ComponentSupport.translate(request.kind() == WaypointPageKind.HOMES
				? "commands.teleport_commands.home.manageTitle"
				: "commands.teleport_commands.warp.manageTitle", request.language())
				.withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
		header.append(Component.literal(" ==========").withStyle(ChatFormatting.DARK_GRAY));
		return header;
	}

	private void appendSummary(MutableComponent message, WaypointPageRequest request, NamedLocationView location) {
		message.append("\n");
		message.append(Component.literal("  - " + location.getName()).withStyle(ChatFormatting.AQUA));
		if (request.kind() == WaypointPageKind.HOMES && Objects.equals(request.defaultLocationUuid(), location.getUuid())) {
			appendMarker(message, request.language(), "commands.teleport_commands.common.default", ChatFormatting.AQUA,
					ChatFormatting.BOLD);
		}
		if (request.kind() == WaypointPageKind.HOMES && location.isTemporary()) {
			appendMarker(message, request.language(), "commands.teleport_commands.home.temporary", ChatFormatting.GOLD,
					ChatFormatting.BOLD);
		}
		boolean mapVisible = isMapVisible(request, location);
		appendMarker(message, request.language(), mapVisible
				? "commands.teleport_commands.common.mapVisible"
				: "commands.teleport_commands.common.mapHidden",
				mapVisible ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY);
		if (request.kind() == WaypointPageKind.WARPS) {
			appendMarker(message, request.language(), location.isVisible()
					? "commands.teleport_commands.gwarpmap.globalVisible"
					: "commands.teleport_commands.gwarpmap.globalHidden",
					location.isVisible() ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY);
		}

		String coords = String.format(Locale.ROOT, "[X%d Y%d Z%d]", location.getX(), location.getY(), location.getZ());
		message.append("\n");
		message.append(Component.literal("     | ").withStyle(ChatFormatting.AQUA));
		message.append(Component.literal(coords).withStyle(ChatFormatting.LIGHT_PURPLE));
		message.append(Component.literal(" [" + location.getDimensionId() + "]").withStyle(ChatFormatting.DARK_PURPLE));
	}

	private void appendActions(MutableComponent message, WaypointPageRequest request, NamedLocationView location) {
		String quotedName = CommandArgumentUtils.quote(location.getName());
		int page = request.query().page();
		message.append("\n");
		message.append(Component.literal("     | ").withStyle(ChatFormatting.AQUA));
		appendButton(message, request.language(), "commands.teleport_commands.common.tp", ChatFormatting.GREEN,
				new ClickEvent.RunCommand(commands.teleportCommand(request.kind(), quotedName)));
		appendButton(message, request.language(), "commands.teleport_commands.common.rename", ChatFormatting.BLUE,
				new ClickEvent.SuggestCommand("/" + commands.renameCommand(request.kind()) + " " + quotedName + " "));
		appendButton(message, request.language(), "commands.teleport_commands.common.update", ChatFormatting.YELLOW,
				new ClickEvent.RunCommand(commands.manageUpdateCommand(request, location.getUuid(), page)));
		if (request.kind() == WaypointPageKind.HOMES && !location.isTemporary()
				&& !Objects.equals(request.defaultLocationUuid(), location.getUuid())) {
			appendButton(message, request.language(), "commands.teleport_commands.common.defaultPrompt", ChatFormatting.DARK_AQUA,
					new ClickEvent.RunCommand(commands.manageDefaultCommand(request, location.getUuid(), page)));
		}
		if (request.kind() == WaypointPageKind.HOMES && !location.isTemporary()) {
			message.append("\n");
			message.append(Component.literal("     | ").withStyle(ChatFormatting.AQUA));
			if (request.publishedHomeUuids().contains(location.getUuid())) {
				appendButton(message, request.language(), "commands.teleport_commands.sharedhome.broadcast", ChatFormatting.GOLD,
						new ClickEvent.RunCommand(commands.manageShareCommand(request, location.getUuid(), page)));
				appendButton(message, request.language(), "commands.teleport_commands.sharedhome.withdraw", ChatFormatting.RED,
						new ClickEvent.RunCommand(commands.manageWithdrawCommand(request, location.getUuid(), page)));
			} else {
				appendButton(message, request.language(), "commands.teleport_commands.sharedhome.share", ChatFormatting.GOLD,
						new ClickEvent.RunCommand(commands.manageShareCommand(request, location.getUuid(), page)));
			}
		}

		message.append("\n");
		message.append(Component.literal("     | ").withStyle(ChatFormatting.AQUA));
		appendButton(message, request.language(), "commands.teleport_commands.common.delete", ChatFormatting.RED,
				new ClickEvent.RunCommand(commands.deleteConfirmationCommand(request, location.getUuid(), page)));
		appendButton(message, request.language(), "commands.teleport_commands.common.back", ChatFormatting.AQUA,
				new ClickEvent.RunCommand(commands.listCommand(request.kind(), request.query(), page)));
	}

	private MutableComponent button(String language, String key, ChatFormatting color, String command) {
		return ComponentSupport.translatedButton(language, key, color, new ClickEvent.RunCommand(command));
	}

	private void appendButton(MutableComponent message, String language, String key, ChatFormatting color,
			ClickEvent clickEvent) {
		message.append(ComponentSupport.translatedButton(language, key, color, clickEvent));
		message.append(" ");
	}

	private void appendMarker(MutableComponent message, String language, String key, ChatFormatting... formatting) {
		message.append(" ");
		message.append(ComponentSupport.translate(key, language).withStyle(formatting));
	}

	private boolean isMapVisible(WaypointPageRequest request, NamedLocationView location) {
		if (request.kind() == WaypointPageKind.WARPS) {
			return location.isVisible() && !request.hiddenWarpUuids().contains(location.getUuid());
		}
		return location.isVisible();
	}
}
