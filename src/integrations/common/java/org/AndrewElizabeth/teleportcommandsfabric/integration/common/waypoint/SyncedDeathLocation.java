package org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint;

public record SyncedDeathLocation(String worldId, int x, int y, int z) {
	public static final SyncedDeathLocation NONE = new SyncedDeathLocation("", 0, 0, 0);

	public SyncedDeathLocation {
		worldId = worldId == null ? "" : worldId;
	}

	public boolean hasLocation() {
		return !worldId.isBlank();
	}
}
