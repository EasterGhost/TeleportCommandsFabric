package org.AndrewElizabeth.teleportcommandsfabric.integration.common.network;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.legacy.LegacyXaeroSyncPackets;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol.ClientIntegrationHelloPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol.MapWaypointSnapshotPayload;
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

	public static void writeSnapshot(FriendlyByteBuf buf, MapWaypointSnapshot snapshot, int protocolVersion) {
		buf.writeBoolean(snapshot.persistWaypointSets());
		buf.writeUtf(snapshot.warpGroupName());
		buf.writeUtf(snapshot.homeGroupName());
		buf.writeUtf(snapshot.deathLocation().worldId());
		buf.writeInt(snapshot.deathLocation().x());
		buf.writeInt(snapshot.deathLocation().y());
		buf.writeInt(snapshot.deathLocation().z());
		List<SyncedMapWaypoint> compatibleWaypoints = snapshot.waypoints().stream()
				.filter(waypoint -> protocolVersion >= 2 || waypoint.kind() != SyncedWaypointKind.SHARED_HOME)
				.toList();
		buf.writeVarInt(compatibleWaypoints.size());
		for (SyncedMapWaypoint waypoint : compatibleWaypoints) {
			buf.writeEnum(waypoint.kind());
			buf.writeUtf(waypoint.name());
			if (protocolVersion >= 2) {
				buf.writeUtf(waypoint.commandTarget());
			}
			buf.writeUtf(waypoint.worldId());
			buf.writeInt(waypoint.x());
			buf.writeInt(waypoint.y());
			buf.writeInt(waypoint.z());
		}
	}

	public static MapWaypointSnapshot readSnapshot(FriendlyByteBuf buf, int protocolVersion) {
		boolean persistWaypointSets = buf.readBoolean();
		String warpGroupName = buf.readUtf();
		String homeGroupName = buf.readUtf();
		SyncedDeathLocation deathLocation = new SyncedDeathLocation(
				buf.readUtf(),
				buf.readInt(),
				buf.readInt(),
				buf.readInt());
		int size = buf.readVarInt();
		if (size < 0) {
			throw new DecoderException("Negative waypoint count: " + size);
		}
		List<SyncedMapWaypoint> waypoints = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			SyncedWaypointKind kind = buf.readEnum(SyncedWaypointKind.class);
			String name = buf.readUtf();
			String commandTarget = protocolVersion >= 2 ? buf.readUtf() : name;
			waypoints.add(new SyncedMapWaypoint(
					kind,
					name,
					commandTarget,
					buf.readUtf(),
					buf.readInt(),
					buf.readInt(),
					buf.readInt()));
		}
		return new MapWaypointSnapshot(waypoints, persistWaypointSets, warpGroupName, homeGroupName, deathLocation);
	}
}
