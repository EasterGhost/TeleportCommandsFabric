package org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.IntegrationProtocol;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.MapSyncPackets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ClientIntegrationHelloPayload(int protocolVersion) implements CustomPacketPayload {
	public static final Type<ClientIntegrationHelloPayload> TYPE = new Type<>(MapSyncPackets.HELLO_ID);
	public static final StreamCodec<FriendlyByteBuf, ClientIntegrationHelloPayload> CODEC = StreamCodec.of(
			(buf, value) -> buf.writeVarInt(value.protocolVersion()),
			buf -> new ClientIntegrationHelloPayload(buf.readVarInt()));

	public static ClientIntegrationHelloPayload current() {
		return new ClientIntegrationHelloPayload(IntegrationProtocol.PROTOCOL_VERSION);
	}

	@Override
	public Type<ClientIntegrationHelloPayload> type() {
		return TYPE;
	}
}
