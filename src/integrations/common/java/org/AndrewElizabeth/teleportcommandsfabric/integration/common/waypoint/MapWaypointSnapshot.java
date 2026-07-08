package org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint;

import java.util.List;

public record MapWaypointSnapshot(List<SyncedMapWaypoint> waypoints,
		boolean persistWaypointSets, String warpGroupName, String homeGroupName,
		SyncedDeathLocation deathLocation) {
	private static final MapWaypointSnapshot EMPTY = new MapWaypointSnapshot(List.of(), true, "Default", "Default",
			SyncedDeathLocation.NONE);

	public MapWaypointSnapshot {
		waypoints = List.copyOf(waypoints);
		deathLocation = deathLocation == null ? SyncedDeathLocation.NONE : deathLocation;
	}

	public static MapWaypointSnapshot empty() {
		return EMPTY;
	}
}
