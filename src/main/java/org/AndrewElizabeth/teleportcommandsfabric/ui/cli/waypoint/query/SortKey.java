package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query;

public enum SortKey {
	NAME,
	SEQUENCE;

	public static SortKey defaultKey() {
		return SEQUENCE;
	}
}
