package org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.legacy;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedDeathLocation;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedMapWaypoint;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedWaypointKind;
import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyXaeroSyncPacketsTest {
	@Test
	void legacyPayloadRoundTripsHomeAndWarpFields() {
		MapWaypointSnapshot source = new MapWaypointSnapshot(List.of(
				new SyncedMapWaypoint(SyncedWaypointKind.WARP, "spawn", "minecraft:overworld", 10, 64, 20),
				new SyncedMapWaypoint(SyncedWaypointKind.HOME, "base", "minecraft:the_nether", -5, 70, 8)),
				false, "Warps", "Homes",
				new SyncedDeathLocation("minecraft:overworld", 1, 2, 3));

		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		LegacyXaeroSyncPackets.writeSnapshot(buffer, source);

		MapWaypointSnapshot decoded = LegacyXaeroSyncPackets.readSnapshot(buffer);

		assertEquals(false, decoded.persistWaypointSets());
		assertEquals("Warps", decoded.warpGroupName());
		assertEquals("Homes", decoded.homeGroupName());
		assertEquals(SyncedDeathLocation.NONE, decoded.deathLocation());
		assertEquals(source.waypoints(), decoded.waypoints());
	}
}
