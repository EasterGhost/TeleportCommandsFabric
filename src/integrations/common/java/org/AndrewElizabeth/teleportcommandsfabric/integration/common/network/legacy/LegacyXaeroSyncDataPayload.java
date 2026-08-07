package org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.legacy;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LegacyXaeroSyncDataPayload(MapWaypointSnapshot snapshot) implements CustomPacketPayload {
	public static final Type<LegacyXaeroSyncDataPayload> TYPE =
			new Type<>(LegacyXaeroSyncPackets.SYNC_DATA_ID);
	public static final StreamCodec<FriendlyByteBuf, LegacyXaeroSyncDataPayload> CODEC = StreamCodec.of(
			(buf, value) -> LegacyXaeroSyncPackets.writeSnapshot(buf, value.snapshot()),
			buf -> new LegacyXaeroSyncDataPayload(LegacyXaeroSyncPackets.readSnapshot(buf)));

	@Override
	public Type<LegacyXaeroSyncDataPayload> type() {
		return TYPE;
	}
}
