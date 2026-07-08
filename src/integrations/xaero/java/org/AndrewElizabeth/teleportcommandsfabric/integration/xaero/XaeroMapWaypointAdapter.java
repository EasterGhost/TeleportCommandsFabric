package org.AndrewElizabeth.teleportcommandsfabric.integration.xaero;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.client.MapWaypointAdapter;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;

public final class XaeroMapWaypointAdapter implements MapWaypointAdapter {
	@Override
	public String id() {
		return "xaero";
	}

	@Override
	public boolean applySnapshot(MapWaypointSnapshot snapshot) {
		return XaeroCompat.applySnapshot(snapshot);
	}
}
