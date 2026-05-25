package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model;

public record WaypointListQuery(int page, WaypointFilter filter, WaypointSort sort) {
	public static final int DEFAULT_PAGE = 1;

	public WaypointListQuery {
		page = Math.max(DEFAULT_PAGE, page);
		filter = filter == null ? WaypointFilter.none() : filter;
		sort = sort == null ? WaypointSort.DEFAULT : sort;
	}

	public static WaypointListQuery defaultQuery() {
		return new WaypointListQuery(DEFAULT_PAGE, WaypointFilter.none(), WaypointSort.DEFAULT);
	}
}
