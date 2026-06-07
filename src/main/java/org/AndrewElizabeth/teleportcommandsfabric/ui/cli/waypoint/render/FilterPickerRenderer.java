package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.render;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.ComponentSupport;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointFilterPickerKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageRequest;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointFilter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;

public final class FilterPickerRenderer {
	private static final int PREFIX_COLUMNS = 9;
	private static final int DIMENSION_COLUMNS = 4;

	private final CommandLinkBuilder commands;

	public FilterPickerRenderer(CommandLinkBuilder commands) {
		this.commands = Objects.requireNonNull(commands, "commands");
	}

	public Component render(WaypointPageRequest request, WaypointFilterPickerKind pickerKind) {
		Objects.requireNonNull(request, "request");
		WaypointFilterPickerKind safePickerKind = pickerKind == null ? WaypointFilterPickerKind.PREFIX : pickerKind;
		MutableComponent picker = Component.empty();
		picker.append(title(request, safePickerKind));
		picker.append("\n");
		appendAllButton(picker, request);
		if (safePickerKind == WaypointFilterPickerKind.PREFIX) {
			appendPrefixButtons(picker, request);
		} else {
			appendDimensionButtons(picker, request);
		}
		picker.append("\n");
		picker.append(ComponentSupport.navButton(request.language(), "commands.teleport_commands.common.back",
				commands.listCommand(request.kind(), request.query(), request.query().page())));
		return picker;
	}

	private MutableComponent title(WaypointPageRequest request, WaypointFilterPickerKind pickerKind) {
		String titleKey = pickerKind == WaypointFilterPickerKind.PREFIX
				? "commands.teleport_commands.common.prefixFilterTitle"
				: "commands.teleport_commands.common.dimensionFilterTitle";
		return Component.literal("========== ").withStyle(ChatFormatting.DARK_GRAY)
				.append(ComponentSupport.translate(titleKey, request.language(),
						ComponentSupport.translate(ComponentSupport.waypointTitleKey(request.kind()), request.language()))
						.withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
				.append(Component.literal(" ==========").withStyle(ChatFormatting.DARK_GRAY));
	}

	private void appendAllButton(MutableComponent picker, WaypointPageRequest request) {
		boolean active = request.query().filter().isNone();
		picker.append(WaypointRenderSupport.stateButton(ComponentSupport.translate("commands.teleport_commands.common.all", request.language()),
				commands.clearFilterCommand(request.kind(), request.query()), active));
	}

	private void appendPrefixButtons(MutableComponent picker, WaypointPageRequest request) {
		for (char letter = 'a'; letter <= 'z'; letter++) {
			int index = letter - 'a';
			picker.append(index % PREFIX_COLUMNS == 0 ? "\n" : " ");
			String value = String.valueOf(letter);
			boolean active = request.query().filter() instanceof WaypointFilter.Prefix prefix
					&& prefix.value().equals(value);
			picker.append(WaypointRenderSupport.stateButton(Component.literal(value.toUpperCase(Locale.ROOT)),
					commands.prefixFilterCommand(request.kind(), request.query(), value), active));
		}
	}

	private void appendDimensionButtons(MutableComponent picker, WaypointPageRequest request) {
		TreeSet<String> dimensions = new TreeSet<>();
		for (NamedLocationView location : request.locations()) {
			String dimensionId = WaypointFilter.normalize(location.getDimensionId());
			if (!dimensionId.isEmpty()) {
				dimensions.add(dimensionId);
			}
		}
		int index = 0;
		for (String dimensionId : dimensions) {
			picker.append(index % DIMENSION_COLUMNS == 0 ? "\n" : " ");
			boolean active = request.query().filter() instanceof WaypointFilter.Dimension dimension
					&& dimension.dimensionId().equals(dimensionId);
			picker.append(WaypointRenderSupport.stateButton(Component.literal(WaypointRenderSupport.shortDimensionId(dimensionId)),
					commands.dimensionFilterCommand(request.kind(), request.query(), dimensionId), active));
			index++;
		}
	}
}
