package org.AndrewElizabeth.teleportcommandsfabric.integration.common.client;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;

public final class ClientMapWaypointSnapshots {
	private static volatile MapWaypointSnapshot latest = MapWaypointSnapshot.empty();
	private static volatile boolean latestFromLegacyXaero;

	private ClientMapWaypointSnapshots() {
	}

	public static MapWaypointSnapshot latest() {
		return latest;
	}

	public static boolean latestFromLegacyXaero() {
		return latestFromLegacyXaero;
	}

	static void update(MapWaypointSnapshot snapshot) {
		update(snapshot, false);
	}

	static void updateLegacyXaero(MapWaypointSnapshot snapshot) {
		update(snapshot, true);
	}

	private static void update(MapWaypointSnapshot snapshot, boolean legacyXaero) {
		latest = snapshot == null ? MapWaypointSnapshot.empty() : snapshot;
		latestFromLegacyXaero = legacyXaero;
	}

	static void clear() {
		latest = MapWaypointSnapshot.empty();
		latestFromLegacyXaero = false;
	}
}
