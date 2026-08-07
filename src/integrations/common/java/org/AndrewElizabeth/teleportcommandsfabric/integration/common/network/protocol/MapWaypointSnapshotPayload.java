package org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.IntegrationProtocol;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.MapSyncPackets;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MapWaypointSnapshotPayload(int protocolVersion, MapWaypointSnapshot snapshot) implements CustomPacketPayload {
	public static final Type<MapWaypointSnapshotPayload> TYPE = new Type<>(MapSyncPackets.SNAPSHOT_ID);
	public static final StreamCodec<FriendlyByteBuf, MapWaypointSnapshotPayload> CODEC = StreamCodec.of(
			(buf, value) -> {
				buf.writeVarInt(value.protocolVersion());
				MapSyncPackets.writeSnapshot(buf, value.snapshot(), value.protocolVersion());
			},
			buf -> {
				int protocolVersion = buf.readVarInt();
				if (!IntegrationProtocol.isSupported(protocolVersion)) {
					throw new DecoderException("Unsupported map waypoint snapshot protocol: " + protocolVersion);
				}
				return new MapWaypointSnapshotPayload(protocolVersion,
						MapSyncPackets.readSnapshot(buf, protocolVersion));
			});

	public static MapWaypointSnapshotPayload current(MapWaypointSnapshot snapshot) {
		return new MapWaypointSnapshotPayload(IntegrationProtocol.PROTOCOL_VERSION, snapshot);
	}

	@Override
	public Type<MapWaypointSnapshotPayload> type() {
		return TYPE;
	}
}
