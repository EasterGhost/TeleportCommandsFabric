package org.AndrewElizabeth.teleportcommandsfabric.integration.common.client;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.IntegrationProtocol;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.MapSyncPackets;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol.ClientIntegrationHelloPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol.MapWaypointSnapshotPayload;
import org.AndrewElizabeth.teleportcommandsfabric.utils.DebugLog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public final class MapWaypointSyncClient {
	private static final int JOIN_HELLO_DELAY_TICKS = 20;
	private static boolean initialized;
	private static boolean pendingHello;
	private static int ticksSinceJoin;

	private MapWaypointSyncClient() {
	}

	static void requestHello() {
		pendingHello = true;
	}

	public static void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;
		DebugLog.info("Integration common sync client initializing.");

		MapSyncPackets.registerPayloadTypes();
		ClientPlayNetworking.registerGlobalReceiver(MapWaypointSnapshotPayload.TYPE,
				(payload, context) -> context.client().execute(() -> handleSnapshot(payload)));

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
			if (ticksSinceJoin >= JOIN_HELLO_DELAY_TICKS && ClientPlayNetworking.canSend(ClientIntegrationHelloPayload.TYPE)) {
				ClientPlayNetworking.send(ClientIntegrationHelloPayload.current());
				pendingHello = false;
				DebugLog.info("Sending integration common hello.");
			}
		}
		MapWaypointAdapterRegistry.retryPending();
	}

	private static void handleSnapshot(MapWaypointSnapshotPayload payload) {
		if (!IntegrationProtocol.isSupported(payload.protocolVersion())) {
			DebugLog.debug("Ignoring unsupported map waypoint snapshot protocol {}.", payload.protocolVersion());
			return;
		}
		ClientMapWaypointSnapshots.update(payload.snapshot());
		DebugLog.info("Map waypoint snapshot received (waypoints: {}).", payload.snapshot().waypoints().size());
		MapWaypointAdapterRegistry.dispatch(payload.snapshot());
	}
}
