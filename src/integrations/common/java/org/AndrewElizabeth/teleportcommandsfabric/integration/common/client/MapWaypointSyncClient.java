package org.AndrewElizabeth.teleportcommandsfabric.integration.common.client;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.IntegrationProtocol;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.MapSyncPackets;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.legacy.LegacyXaeroSyncDataPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.legacy.LegacyXaeroSyncRequestPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol.ClientIntegrationHelloPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol.MapWaypointSnapshotPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.utils.DebugLog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
final class MapWaypointSyncClient {
	private static final int JOIN_HELLO_DELAY_TICKS = 20;
	private static boolean initialized;
	private static boolean pendingHello;
	private static int ticksSinceJoin;

	private MapWaypointSyncClient() {
	}

	static void requestHello() {
		pendingHello = true;
	}

	static void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;
		DebugLog.info("Integration common sync client initializing.");

		MapSyncPackets.registerPayloadTypes();
		ClientPlayNetworking.registerGlobalReceiver(MapWaypointSnapshotPayload.TYPE,
				(payload, context) -> context.client().execute(() -> handleSnapshot(payload)));
		ClientPlayNetworking.registerGlobalReceiver(LegacyXaeroSyncDataPayload.TYPE,
				(payload, context) -> context.client().execute(() -> handleLegacySnapshot(payload)));

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			pendingHello = true;
			ticksSinceJoin = 0;
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			pendingHello = false;
			ticksSinceJoin = 0;
			MapWaypointAdapterRegistry.clearAll();
		});

		ClientTickEvents.END_CLIENT_TICK.register(MapWaypointSyncClient::onClientTick);
	}

	private static void onClientTick(Minecraft client) {
		if (client == null || client.level == null) {
			return;
		}
		if (pendingHello && MapWaypointAdapterRegistry.hasAdapters()) {
			ticksSinceJoin++;
			if (ticksSinceJoin >= JOIN_HELLO_DELAY_TICKS) {
				sendBestAvailableHandshake();
			}
		}
		MapWaypointAdapterRegistry.retryPending();
	}

	private static void sendBestAvailableHandshake() {
		if (ClientPlayNetworking.canSend(ClientIntegrationHelloPayload.TYPE)) {
			ClientPlayNetworking.send(ClientIntegrationHelloPayload.current());
			if (IntegrationProtocol.PROTOCOL_VERSION > IntegrationProtocol.MIN_SUPPORTED_PROTOCOL_VERSION) {
				ClientPlayNetworking.send(new ClientIntegrationHelloPayload(
						IntegrationProtocol.MIN_SUPPORTED_PROTOCOL_VERSION));
			}
			pendingHello = false;
			DebugLog.info("Sending integration common hello.");
			return;
		}
		if (ClientPlayNetworking.canSend(LegacyXaeroSyncRequestPayload.TYPE)) {
			ClientPlayNetworking.send(new LegacyXaeroSyncRequestPayload());
			pendingHello = false;
			DebugLog.info("Sending legacy Xaero sync request.");
		}
	}

	private static void handleSnapshot(MapWaypointSnapshotPayload payload) {
		ClientMapWaypointSnapshots.update(payload.snapshot());
		DebugLog.info("Map waypoint snapshot received (waypoints: {}).", payload.snapshot().waypoints().size());
		MapWaypointAdapterRegistry.dispatch(payload.snapshot());
	}

	private static void handleLegacySnapshot(LegacyXaeroSyncDataPayload payload) {
		MapWaypointSnapshot snapshot = payload.snapshot();
		ClientMapWaypointSnapshots.updateLegacyXaero(snapshot);
		DebugLog.info("Legacy Xaero waypoint snapshot received (waypoints: {}).", snapshot.waypoints().size());
		MapWaypointAdapterRegistry.dispatch(snapshot);
	}
}
