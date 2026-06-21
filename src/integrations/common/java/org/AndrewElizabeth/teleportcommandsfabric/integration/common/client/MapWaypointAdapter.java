package org.AndrewElizabeth.teleportcommandsfabric.integration.common.client;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;

public interface MapWaypointAdapter {
	String id();

	boolean applySnapshot(MapWaypointSnapshot snapshot);

	default void clear() {
		applySnapshot(MapWaypointSnapshot.empty());
	}
}
