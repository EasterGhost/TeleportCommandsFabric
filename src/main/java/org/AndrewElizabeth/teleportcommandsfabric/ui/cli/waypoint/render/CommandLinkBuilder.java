package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.render;

import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeKey;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageRequest;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.SortDirection;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.SortKey;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointFilter;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointSort;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;

import java.util.UUID;

public final class CommandLinkBuilder {
	public String listCommand(WaypointPageKind kind, WaypointListQuery query, int page) {
		return listCommand(kind) + " " + pageQueryArgs(query, page);
	}

	public String pagePickerCommand(WaypointPageKind kind, WaypointListQuery query, int page) {
		return pagePickerCommand(kind) + " " + pageQueryArgs(query, page);
	}

	public String prefixFilterPickerCommand(WaypointPageKind kind, WaypointListQuery query, int page) {
		return prefixFilterPickerCommand(kind) + " " + pageQueryArgs(query, page);
	}

	public String dimensionFilterPickerCommand(WaypointPageKind kind, WaypointListQuery query, int page) {
		return dimensionFilterPickerCommand(kind) + " " + pageQueryArgs(query, page);
	}

	public String clearFilterCommand(WaypointPageKind kind, WaypointListQuery query) {
		WaypointListQuery nextQuery = new WaypointListQuery(1, WaypointFilter.none(), safeQuery(query).sort());
		return listCommand(kind, nextQuery, 1);
	}

	public String prefixFilterCommand(WaypointPageKind kind, WaypointListQuery query, String prefix) {
		WaypointListQuery nextQuery = new WaypointListQuery(1, WaypointFilter.prefix(prefix), safeQuery(query).sort());
		return listCommand(kind, nextQuery, 1);
	}

	public String dimensionFilterCommand(WaypointPageKind kind, WaypointListQuery query, String dimensionId) {
		WaypointListQuery nextQuery = new WaypointListQuery(1, WaypointFilter.dimension(dimensionId), safeQuery(query).sort());
		return listCommand(kind, nextQuery, 1);
	}

	public String sortCommand(WaypointPageKind kind, WaypointListQuery query, SortKey key) {
		WaypointListQuery safeQuery = safeQuery(query);
		WaypointSort currentSort = safeQuery.sort();
		SortDirection direction = currentSort.key() == key ? toggle(currentSort.direction()) : SortDirection.ASC;
		WaypointListQuery nextQuery = new WaypointListQuery(1, safeQuery.filter(), new WaypointSort(key, direction));
		return listCommand(kind, nextQuery, 1);
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

	public String sharedTeleportCommand(SharedHomeKey key) {
		return "teleportcommandsfabric:sharedhome " + key.ownerUuid() + " " + key.homeUuid();
	}

	public String sharedVisibilityCommand(SharedHomeKey key, boolean visible, WaypointListQuery query, int currentPage) {
		return sharedUiCommand("map", key, visible + " " + pageQueryArgs(query, currentPage));
	}

	public String sharedUnsubscribeCommand(SharedHomeKey key, WaypointListQuery query, int currentPage) {
		return sharedUiCommand("unsubscribe", key, pageQueryArgs(query, currentPage));
	}

	public String manageCommand(WaypointPageRequest request, UUID waypointUuid, int currentPage) {
		return uiCommand(request, "manage", waypointUuid, currentPage);
	}

	public String manageUpdateCommand(WaypointPageRequest request, UUID waypointUuid, int currentPage) {
		return uiCommand(request, "update", waypointUuid, currentPage);
	}

	public String manageDefaultCommand(WaypointPageRequest request, UUID waypointUuid, int currentPage) {
		return uiCommand(request, "default", waypointUuid, currentPage);
	}

	public String manageShareCommand(WaypointPageRequest request, UUID waypointUuid, int currentPage) {
		return uiCommand(request, "share", waypointUuid, currentPage);
	}

	public String manageWithdrawCommand(WaypointPageRequest request, UUID waypointUuid, int currentPage) {
		return uiCommand(request, "withdraw", waypointUuid, currentPage);
	}

	public String deleteConfirmationCommand(WaypointPageRequest request, UUID waypointUuid, int currentPage) {
		return uiCommand(request, "delete", waypointUuid, currentPage);
	}

	public String confirmDeleteCommand(WaypointPageRequest request, UUID waypointUuid, int currentPage) {
		return uiCommand(request, "confirmdelete", waypointUuid, currentPage);
	}

	public String renameCommand(WaypointPageKind kind) {
		return kind == WaypointPageKind.HOMES ? "renamehome" : "renamewarp";
	}

	private String pageQueryArgs(WaypointListQuery query, int page) {
		StringBuilder args = new StringBuilder().append(Math.max(1, page));
		appendQuery(args, query);
		return args.toString();
	}

	private void appendQuery(StringBuilder command, WaypointListQuery query) {
		WaypointListQuery safeQuery = safeQuery(query);
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
		return switch (kind) {
		case HOMES -> "homes";
		case WARPS -> "warps";
		case SHARED_HOMES -> "sharedhomes";
		};
	}

	private String pagePickerCommand(WaypointPageKind kind) {
		return switch (kind) {
		case HOMES -> "teleportcommandsfabric:homespages";
		case WARPS -> "teleportcommandsfabric:warpspages";
		case SHARED_HOMES -> "teleportcommandsfabric:sharedhomespages";
		};
	}

	private String prefixFilterPickerCommand(WaypointPageKind kind) {
		return switch (kind) {
		case HOMES -> "teleportcommandsfabric:homesprefixfilters";
		case WARPS -> "teleportcommandsfabric:warpsprefixfilters";
		case SHARED_HOMES -> "teleportcommandsfabric:sharedhomesprefixfilters";
		};
	}

	private String dimensionFilterPickerCommand(WaypointPageKind kind) {
		return switch (kind) {
		case HOMES -> "teleportcommandsfabric:homesdimensionfilters";
		case WARPS -> "teleportcommandsfabric:warpsdimensionfilters";
		case SHARED_HOMES -> "teleportcommandsfabric:sharedhomesdimensionfilters";
		};
	}

	private String visibilityCommand(WaypointPageKind kind) {
		return kind == WaypointPageKind.HOMES ? "teleportcommandsfabric:maphome" : "teleportcommandsfabric:mapwarp";
	}

	private String uiCommand(WaypointPageRequest request, String action, UUID waypointUuid, int currentPage) {
		String root = request.kind() == WaypointPageKind.HOMES
				? "teleportcommandsfabric:homeui"
				: "teleportcommandsfabric:warpui";
		return root + " " + action + " " + waypointUuid + " " + pageQueryArgs(request.query(), currentPage);
	}

	private String sharedUiCommand(String action, SharedHomeKey key, String suffix) {
		return "teleportcommandsfabric:sharedhomeui " + action + " " + key.ownerUuid() + " " + key.homeUuid()
				+ " " + suffix;
	}

	private WaypointListQuery safeQuery(WaypointListQuery query) {
		return query == null ? WaypointListQuery.defaultQuery() : query;
	}

	private SortDirection toggle(SortDirection direction) {
		return direction == SortDirection.ASC ? SortDirection.DESC : SortDirection.ASC;
	}
}
