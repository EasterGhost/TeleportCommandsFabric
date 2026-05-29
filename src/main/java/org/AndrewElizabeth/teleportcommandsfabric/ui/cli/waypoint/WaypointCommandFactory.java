package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.*;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;

public final class WaypointCommandFactory {
	public String listCommand(WaypointPageKind kind, WaypointListQuery query, int page) {
		StringBuilder command = new StringBuilder(listCommand(kind)).append(' ').append(Math.max(1, page));
		appendQuery(command, query);
		return command.toString();
	}

	public String pagePickerCommand(WaypointPageKind kind, WaypointListQuery query, int page) {
		StringBuilder command = new StringBuilder(pagePickerCommand(kind)).append(' ').append(Math.max(1, page));
		appendQuery(command, query);
		return command.toString();
	}

	public String visibilityCommand(WaypointPageRequest request, String quotedName, boolean visible, int currentPage) {
		String listCommand = listCommand(request.kind(), request.query(), currentPage);
		String listArgs = listCommand.substring(listCommand(request.kind()).length()).trim();
		return visibilityCommand(request.kind()) + " " + quotedName + " " + visible + " " + listArgs;
	}

	public String globalWarpVisibilityCommand(String quotedName, boolean visible) {
		return "gwarpmap " + quotedName + " " + visible;
	}

	public String teleportCommand(WaypointPageKind kind, String quotedName) {
		return (kind == WaypointPageKind.HOMES ? "home " : "warp ") + quotedName;
	}

	public String renameCommand(WaypointPageKind kind) {
		return kind == WaypointPageKind.HOMES ? "renamehome" : "renamewarp";
	}

	public String updateCommand(WaypointPageKind kind) {
		return kind == WaypointPageKind.HOMES ? "updatehome" : "updatewarp";
	}

	public String deleteCommand(WaypointPageKind kind) {
		return kind == WaypointPageKind.HOMES ? "delhome" : "delwarp";
	}

	private void appendQuery(StringBuilder command, WaypointListQuery query) {
		WaypointListQuery safeQuery = query == null ? WaypointListQuery.defaultQuery() : query;
		appendFilter(command, safeQuery.filter());
		appendSort(command, safeQuery.sort());
	}

	private void appendFilter(StringBuilder command, WaypointFilter filter) {
		if (filter instanceof WaypointFilter.Prefix prefix) {
			command.append(" filter prefix ").append(CommandArgumentUtils.quote(prefix.value()));
		} else if (filter instanceof WaypointFilter.Dimension dimension) {
			command.append(" filter dimension ").append(CommandArgumentUtils.quote(dimension.dimensionId()));
		}
	}

	private void appendSort(StringBuilder command, WaypointSort sort) {
		if (WaypointSort.DEFAULT.equals(sort)) {
			return;
		}
		command.append(" sort ").append(sort.key() == SortKey.NAME ? "name" : "sequence")
				.append(' ').append(sort.direction() == SortDirection.ASC ? "asc" : "desc");
	}

	private String listCommand(WaypointPageKind kind) {
		return kind == WaypointPageKind.HOMES ? "homes" : "warps";
	}

	private String pagePickerCommand(WaypointPageKind kind) {
		return kind == WaypointPageKind.HOMES ? "teleportcommandsfabric:homespages" : "teleportcommandsfabric:warpspages";
	}

	private String visibilityCommand(WaypointPageKind kind) {
		return kind == WaypointPageKind.HOMES ? "teleportcommandsfabric:maphome" : "teleportcommandsfabric:mapwarp";
	}
}
