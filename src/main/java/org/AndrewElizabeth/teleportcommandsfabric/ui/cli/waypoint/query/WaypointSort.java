package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query;

public record WaypointSort(SortKey key, SortDirection direction) {
	public static final WaypointSort DEFAULT = new WaypointSort(
			SortKey.defaultKey(),
			SortDirection.defaultDirection());

	public WaypointSort {
		key = key == null ? SortKey.defaultKey() : key;
		direction = direction == null ? SortDirection.defaultDirection() : direction;
	}
}
