package org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint;

public record SyncedMapWaypoint(SyncedWaypointKind kind, String name, String commandTarget,
		String worldId, int x, int y, int z) {
	public SyncedMapWaypoint(SyncedWaypointKind kind, String name, String worldId, int x, int y, int z) {
		this(kind, name, name, worldId, x, y, z);
	}
}
