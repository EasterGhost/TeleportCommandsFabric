package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model;

public record WaypointListCacheKey(WaypointFilter filter, WaypointSort sort) {
	public WaypointListCacheKey {
		filter = filter == null ? WaypointFilter.none() : filter;
		sort = sort == null ? WaypointSort.DEFAULT : sort;
	}

	public static WaypointListCacheKey from(WaypointListQuery query) {
		return new WaypointListCacheKey(query.filter(), query.sort());
	}
}
