package org.AndrewElizabeth.teleportcommandsfabric.integration.common.client;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;

public final class ClientMapWaypointSnapshots {
	private static volatile MapWaypointSnapshot latest = MapWaypointSnapshot.empty();

	private ClientMapWaypointSnapshots() {
	}

	public static MapWaypointSnapshot latest() {
		return latest;
	}

	static void update(MapWaypointSnapshot snapshot) {
		latest = snapshot == null ? MapWaypointSnapshot.empty() : snapshot;
	}

	static void clear() {
		latest = MapWaypointSnapshot.empty();
	}
}
