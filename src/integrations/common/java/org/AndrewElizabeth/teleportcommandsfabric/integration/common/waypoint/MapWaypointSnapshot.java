package org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint;

import java.util.List;

public record MapWaypointSnapshot(List<SyncedMapWaypoint> waypoints,
		boolean persistWaypointSets, String warpGroupName, String homeGroupName) {
	private static final MapWaypointSnapshot EMPTY = new MapWaypointSnapshot(List.of(), true, "Default", "Default");

	public MapWaypointSnapshot {
		waypoints = List.copyOf(waypoints);
	}

	public static MapWaypointSnapshot empty() {
		return EMPTY;
	}
}
