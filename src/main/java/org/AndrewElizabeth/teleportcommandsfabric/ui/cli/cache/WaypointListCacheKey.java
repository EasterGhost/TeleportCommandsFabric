package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.cache;

import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointFilter;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointSort;

record WaypointListCacheKey(WaypointFilter filter, WaypointSort sort) {
	WaypointListCacheKey {
		filter = filter == null ? WaypointFilter.none() : filter;
		sort = sort == null ? WaypointSort.DEFAULT : sort;
	}

	static WaypointListCacheKey from(WaypointListQuery query) {
		return new WaypointListCacheKey(query.filter(), query.sort());
	}
}
