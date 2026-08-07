package org.AndrewElizabeth.teleportcommandsfabric.integration.common.server;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapWaypointClientStateTest {
	@Test
	void commonProtocolCannotBeDowngraded() {
		MapWaypointClientState state = new MapWaypointClientState(MapWaypointSyncMode.COMMON, 2);

		assertFalse(state.useCommon(1));
		assertEquals(2, state.protocolVersion());
	}

	@Test
	void commonProtocolUpgradeInvalidatesSentSnapshot() {
		MapWaypointClientState state = new MapWaypointClientState(MapWaypointSyncMode.COMMON, 1);
		MapWaypointSnapshot snapshot = MapWaypointSnapshot.empty();
		state.updateSnapshot(snapshot);

		assertTrue(state.useCommon(2));
		assertEquals(2, state.protocolVersion());
		assertTrue(state.isSnapshotChanged(snapshot));
	}
}
