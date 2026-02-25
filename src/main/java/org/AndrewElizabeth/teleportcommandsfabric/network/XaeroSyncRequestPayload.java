package org.AndrewElizabeth.teleportcommandsfabric.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record XaeroSyncRequestPayload() implements CustomPacketPayload {
	public static final Type<XaeroSyncRequestPayload> TYPE =
			new Type<>(XaeroSyncPackets.SYNC_REQUEST_ID);
	public static final StreamCodec<FriendlyByteBuf, XaeroSyncRequestPayload> CODEC =
			StreamCodec.unit(new XaeroSyncRequestPayload());

	@Override
	public Type<XaeroSyncRequestPayload> type() {
		return TYPE;
	}
}
