package org.AndrewElizabeth.teleportcommandsfabric.network;

import java.util.List;

public record XaeroSyncPayload(
		List<XaeroSyncEntry> warps,
		List<XaeroSyncEntry> homes,
		boolean persistWaypointSets,
		String warpSetName,
		String homeSetName) {
}
