package org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.legacy;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LegacyXaeroSyncRequestPayload() implements CustomPacketPayload {
	public static final Type<LegacyXaeroSyncRequestPayload> TYPE =
			new Type<>(LegacyXaeroSyncPackets.SYNC_REQUEST_ID);
	public static final StreamCodec<FriendlyByteBuf, LegacyXaeroSyncRequestPayload> CODEC =
			StreamCodec.unit(new LegacyXaeroSyncRequestPayload());

	@Override
	public Type<LegacyXaeroSyncRequestPayload> type() {
		return TYPE;
	}
}
