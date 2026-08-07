package org.AndrewElizabeth.teleportcommandsfabric.integration.common.server;

enum MapWaypointSyncMode {
	COMMON("common"),
	LEGACY_XAERO("legacy Xaero");

	private final String logName;

	MapWaypointSyncMode(String logName) {
		this.logName = logName;
	}

	String logName() {
		return logName;
	}
}
