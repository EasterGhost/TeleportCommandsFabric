package org.AndrewElizabeth.teleportcommandsfabric.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record XaeroSyncDataPayload(XaeroSyncPayload payload) implements CustomPacketPayload {
	public static final Type<XaeroSyncDataPayload> TYPE =
			new Type<>(XaeroSyncPackets.SYNC_DATA_ID);
	public static final StreamCodec<FriendlyByteBuf, XaeroSyncDataPayload> CODEC =
			StreamCodec.of(
					(buf, value) -> XaeroSyncPackets.writePayload(buf, value.payload()),
					buf -> new XaeroSyncDataPayload(XaeroSyncPackets.readPayload(buf)));

	@Override
	public Type<XaeroSyncDataPayload> type() {
		return TYPE;
	}
}
