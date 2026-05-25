package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model;

public record WaypointSort(WaypointSortKey key, SortDirection direction) {
	public static final WaypointSort DEFAULT = new WaypointSort(
			WaypointSortKey.defaultKey(),
			SortDirection.defaultDirection());

	public WaypointSort {
		key = key == null ? WaypointSortKey.defaultKey() : key;
		direction = direction == null ? SortDirection.defaultDirection() : direction;
	}
}
