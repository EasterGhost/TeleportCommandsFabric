package org.AndrewElizabeth.teleportcommandsfabric.integration.common.network;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol.MapWaypointSnapshotPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedDeathLocation;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedMapWaypoint;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedWaypointKind;
import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MapSyncPacketsTest {
	@Test
	void protocolTwoRoundTripsStableCommandTargets() {
		MapWaypointSnapshot source = snapshot();
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

		MapSyncPackets.writeSnapshot(buffer, source, 2);

		assertEquals(source, MapSyncPackets.readSnapshot(buffer, 2));
	}

	@Test
	void protocolOneOmitsSharedHomesAndUsesNamesAsTargets() {
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

		MapSyncPackets.writeSnapshot(buffer, snapshot(), 1);
		MapWaypointSnapshot decoded = MapSyncPackets.readSnapshot(buffer, 1);

		assertEquals(2, decoded.waypoints().size());
		assertEquals(List.of(SyncedWaypointKind.HOME, SyncedWaypointKind.WARP),
				decoded.waypoints().stream().map(SyncedMapWaypoint::kind).toList());
		assertEquals(List.of("base", "spawn"),
				decoded.waypoints().stream().map(SyncedMapWaypoint::commandTarget).toList());
	}

	@Test
	void unsupportedProtocolIsRejectedBeforeSnapshotBodyIsRead() {
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		buffer.writeVarInt(IntegrationProtocol.PROTOCOL_VERSION + 1);
		buffer.writeByte(0x7F);

		assertThrows(DecoderException.class, () -> MapWaypointSnapshotPayload.CODEC.decode(buffer));
		assertEquals(1, buffer.readerIndex());
	}

	private static MapWaypointSnapshot snapshot() {
		return new MapWaypointSnapshot(List.of(
				new SyncedMapWaypoint(SyncedWaypointKind.HOME, "base", "base",
						"minecraft:overworld", 1, 64, 2),
				new SyncedMapWaypoint(SyncedWaypointKind.WARP, "spawn", "spawn",
						"minecraft:overworld", 3, 65, 4),
				new SyncedMapWaypoint(SyncedWaypointKind.SHARED_HOME, "Alex / farm",
						"00000000-0000-0000-0000-000000000001 00000000-0000-0000-0000-000000000002",
						"minecraft:overworld", 5, 66, 6)),
				false, "Warps", "Homes", new SyncedDeathLocation("minecraft:overworld", 7, 68, 9));
	}
}
