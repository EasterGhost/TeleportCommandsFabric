package org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.legacy;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedDeathLocation;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedMapWaypoint;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedWaypointKind;

import io.netty.handler.codec.DecoderException;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class LegacyXaeroSyncPackets {
	public static final Identifier SYNC_REQUEST_ID =
			Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "xaero_sync_request");
	public static final Identifier SYNC_DATA_ID =
			Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "xaero_sync_data");

	private static boolean payloadTypesRegistered;

	private LegacyXaeroSyncPackets() {
	}

	public static synchronized void registerPayloadTypes() {
		if (payloadTypesRegistered) {
			return;
		}
		PayloadTypeRegistry.playC2S().register(LegacyXaeroSyncRequestPayload.TYPE,
				LegacyXaeroSyncRequestPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(LegacyXaeroSyncDataPayload.TYPE,
				LegacyXaeroSyncDataPayload.CODEC);
		payloadTypesRegistered = true;
	}

	static void writeSnapshot(FriendlyByteBuf buf, MapWaypointSnapshot snapshot) {
		buf.writeBoolean(snapshot.persistWaypointSets());
		buf.writeUtf(snapshot.warpGroupName());
		buf.writeUtf(snapshot.homeGroupName());
		writeEntries(buf, snapshot.waypoints(), SyncedWaypointKind.WARP);
		writeEntries(buf, snapshot.waypoints(), SyncedWaypointKind.HOME);
	}

	static MapWaypointSnapshot readSnapshot(FriendlyByteBuf buf) {
		boolean persistWaypointSets = buf.readBoolean();
		String warpGroupName = buf.readUtf();
		String homeGroupName = buf.readUtf();
		List<SyncedMapWaypoint> waypoints = new ArrayList<>();
		readEntries(buf, waypoints, SyncedWaypointKind.WARP);
		readEntries(buf, waypoints, SyncedWaypointKind.HOME);
		return new MapWaypointSnapshot(waypoints, persistWaypointSets, warpGroupName, homeGroupName,
				SyncedDeathLocation.NONE);
	}

	private static void writeEntries(FriendlyByteBuf buf, List<SyncedMapWaypoint> waypoints,
			SyncedWaypointKind kind) {
		int count = 0;
		for (SyncedMapWaypoint waypoint : waypoints) {
			if (waypoint.kind() == kind) {
				count++;
			}
		}
		buf.writeVarInt(count);
		for (SyncedMapWaypoint waypoint : waypoints) {
			if (waypoint.kind() != kind) {
				continue;
			}
			buf.writeUtf(waypoint.name());
			buf.writeUtf(waypoint.worldId());
			buf.writeInt(waypoint.x());
			buf.writeInt(waypoint.y());
			buf.writeInt(waypoint.z());
		}
	}

	private static void readEntries(FriendlyByteBuf buf, List<SyncedMapWaypoint> waypoints,
			SyncedWaypointKind kind) {
		int size = buf.readVarInt();
		if (size < 0) {
			throw new DecoderException("Negative " + kind.name() + " waypoint count: " + size);
		}
		for (int i = 0; i < size; i++) {
			waypoints.add(new SyncedMapWaypoint(
					kind,
					buf.readUtf(),
					buf.readUtf(),
					buf.readInt(),
					buf.readInt(),
					buf.readInt()));
		}
	}
}
