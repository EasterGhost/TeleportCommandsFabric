package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model;

public enum WaypointSortKey {
	NAME,
	SEQUENCE;

	public static WaypointSortKey defaultKey() {
		return SEQUENCE;
	}
}
