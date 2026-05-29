package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.cache;

import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointFilter;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointSort;

public record WaypointListCacheKey(WaypointFilter filter, WaypointSort sort) {
	public WaypointListCacheKey {
		filter = filter == null ? WaypointFilter.none() : filter;
		sort = sort == null ? WaypointSort.DEFAULT : sort;
	}

	public static WaypointListCacheKey from(WaypointListQuery query) {
		return new WaypointListCacheKey(query.filter(), query.sort());
	}
}
