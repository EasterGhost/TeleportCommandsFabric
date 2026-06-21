package org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.IntegrationProtocol;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.MapSyncPackets;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MapWaypointSnapshotPayload(int protocolVersion, MapWaypointSnapshot snapshot) implements CustomPacketPayload {
	public static final Type<MapWaypointSnapshotPayload> TYPE = new Type<>(MapSyncPackets.SNAPSHOT_ID);
	public static final StreamCodec<FriendlyByteBuf, MapWaypointSnapshotPayload> CODEC = StreamCodec.of(
			(buf, value) -> {
				buf.writeVarInt(value.protocolVersion());
				MapSyncPackets.writeSnapshot(buf, value.snapshot());
			},
			buf -> new MapWaypointSnapshotPayload(buf.readVarInt(), MapSyncPackets.readSnapshot(buf)));

	public static MapWaypointSnapshotPayload current(MapWaypointSnapshot snapshot) {
		return new MapWaypointSnapshotPayload(IntegrationProtocol.PROTOCOL_VERSION, snapshot);
	}

	@Override
	public Type<MapWaypointSnapshotPayload> type() {
		return TYPE;
	}
}
