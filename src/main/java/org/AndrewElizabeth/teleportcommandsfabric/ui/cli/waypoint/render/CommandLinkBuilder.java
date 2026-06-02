package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.render;

import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageRequest;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.SortDirection;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.SortKey;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointFilter;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointSort;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;

public final class CommandLinkBuilder {
	public String listCommand(WaypointPageKind kind, WaypointListQuery query, int page) {
		return listCommand(kind) + " " + pageQueryArgs(query, page);
	}

	public String pagePickerCommand(WaypointPageKind kind, WaypointListQuery query, int page) {
		return pagePickerCommand(kind) + " " + pageQueryArgs(query, page);
	}

	public String visibilityCommand(WaypointPageRequest request, String quotedName, boolean visible, int currentPage) {
		return visibilityCommand(request.kind()) + " " + quotedName + " " + visible + " "
				+ pageQueryArgs(request.query(), currentPage);
	}

	public String globalWarpVisibilityCommand(WaypointPageRequest request, String quotedName, boolean visible, int currentPage) {
		return "teleportcommandsfabric:gmapwarp " + quotedName + " " + visible + " "
				+ pageQueryArgs(request.query(), currentPage);
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

	private String pageQueryArgs(WaypointListQuery query, int page) {
		StringBuilder args = new StringBuilder().append(Math.max(1, page));
		appendQuery(args, query);
		return args.toString();
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
