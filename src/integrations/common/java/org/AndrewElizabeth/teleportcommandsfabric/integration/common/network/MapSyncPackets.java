package org.AndrewElizabeth.teleportcommandsfabric.integration.common.network;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.legacy.LegacyXaeroSyncPackets;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol.ClientIntegrationHelloPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol.MapWaypointSnapshotPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedDeathLocation;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedMapWaypoint;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedWaypointKind;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class MapSyncPackets {
	public static final Identifier HELLO_ID =
			Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "integration_hello");
	public static final Identifier SNAPSHOT_ID =
			Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "map_waypoint_snapshot");

	private static boolean payloadTypesRegistered;

	private MapSyncPackets() {
	}

	public static synchronized void registerPayloadTypes() {
		if (payloadTypesRegistered) {
			return;
		}
		PayloadTypeRegistry.serverboundPlay().register(ClientIntegrationHelloPayload.TYPE, ClientIntegrationHelloPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(MapWaypointSnapshotPayload.TYPE, MapWaypointSnapshotPayload.CODEC);
		LegacyXaeroSyncPackets.registerPayloadTypes();
		payloadTypesRegistered = true;
	}

	public static void writeSnapshot(FriendlyByteBuf buf, MapWaypointSnapshot snapshot) {
		buf.writeBoolean(snapshot.persistWaypointSets());
		buf.writeUtf(snapshot.warpGroupName());
		buf.writeUtf(snapshot.homeGroupName());
		buf.writeUtf(snapshot.deathLocation().worldId());
		buf.writeInt(snapshot.deathLocation().x());
		buf.writeInt(snapshot.deathLocation().y());
		buf.writeInt(snapshot.deathLocation().z());
		buf.writeVarInt(snapshot.waypoints().size());
		for (SyncedMapWaypoint waypoint : snapshot.waypoints()) {
			buf.writeEnum(waypoint.kind());
			buf.writeUtf(waypoint.name());
			buf.writeUtf(waypoint.worldId());
			buf.writeInt(waypoint.x());
			buf.writeInt(waypoint.y());
			buf.writeInt(waypoint.z());
		}
	}

	public static MapWaypointSnapshot readSnapshot(FriendlyByteBuf buf) {
		boolean persistWaypointSets = buf.readBoolean();
		String warpGroupName = buf.readUtf();
		String homeGroupName = buf.readUtf();
		SyncedDeathLocation deathLocation = new SyncedDeathLocation(
				buf.readUtf(),
				buf.readInt(),
				buf.readInt(),
				buf.readInt());
		int size = buf.readVarInt();
		List<SyncedMapWaypoint> waypoints = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			waypoints.add(new SyncedMapWaypoint(
					buf.readEnum(SyncedWaypointKind.class),
					buf.readUtf(),
					buf.readUtf(),
					buf.readInt(),
					buf.readInt(),
					buf.readInt()));
		}
		return new MapWaypointSnapshot(waypoints, persistWaypointSets, warpGroupName, homeGroupName, deathLocation);
	}
}
