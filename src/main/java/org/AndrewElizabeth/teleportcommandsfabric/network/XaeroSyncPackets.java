package org.AndrewElizabeth.teleportcommandsfabric.network;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class XaeroSyncPackets {
	public static final Identifier SYNC_REQUEST_ID =
				Identifier.fromNamespaceAndPath(Constants.MOD_ID, "xaero_sync_request");
	public static final Identifier SYNC_DATA_ID =
				Identifier.fromNamespaceAndPath(Constants.MOD_ID, "xaero_sync_data");

	private XaeroSyncPackets() {
	}

	public static void registerPayloadTypes() {
		PayloadTypeRegistry.playC2S().register(XaeroSyncRequestPayload.TYPE, XaeroSyncRequestPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(XaeroSyncDataPayload.TYPE, XaeroSyncDataPayload.CODEC);
	}

	public static void writePayload(FriendlyByteBuf buf, XaeroSyncPayload payload) {
		buf.writeBoolean(payload.persistWaypointSets());
		buf.writeUtf(payload.warpSetName());
		buf.writeUtf(payload.homeSetName());
		writeEntries(buf, payload.warps());
		writeEntries(buf, payload.homes());
	}

	public static XaeroSyncPayload readPayload(FriendlyByteBuf buf) {
		boolean persist = buf.readBoolean();
		String warpSetName = buf.readUtf();
		String homeSetName = buf.readUtf();
		List<XaeroSyncEntry> warps = readEntries(buf);
		List<XaeroSyncEntry> homes = readEntries(buf);
		return new XaeroSyncPayload(warps, homes, persist, warpSetName, homeSetName);
	}

	private static void writeEntries(FriendlyByteBuf buf, List<XaeroSyncEntry> entries) {
		buf.writeVarInt(entries.size());
		for (XaeroSyncEntry entry : entries) {
			buf.writeUtf(entry.name());
			buf.writeUtf(entry.worldId());
			buf.writeInt(entry.x());
			buf.writeInt(entry.y());
			buf.writeInt(entry.z());
		}
	}

	private static List<XaeroSyncEntry> readEntries(FriendlyByteBuf buf) {
		int size = buf.readVarInt();
		List<XaeroSyncEntry> entries = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			entries.add(new XaeroSyncEntry(
					buf.readUtf(),
					buf.readUtf(),
					buf.readInt(),
					buf.readInt(),
					buf.readInt()));
		}
		return entries;
	}
}
