package org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.client;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.network.XaeroSyncPackets;
import org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.network.protocol.XaeroSyncDataPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.network.protocol.XaeroSyncPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.network.protocol.XaeroSyncRequestPayload;
import org.AndrewElizabeth.teleportcommandsfabric.utils.DebugLog;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public final class XaeroSyncClient {
	private static final String WORLD_MAP_SCREEN_CLASS = "xaero.map.gui.GuiMap";
	private static final long REQUEST_COOLDOWN_MS = ModConstants.SYNC_INTERVAL.toMillis();
	private static final int JOIN_REQUEST_DELAY_TICKS = 40;
	private static boolean initialized;
	private static boolean xaeroAvailable;
	private static boolean wasWorldMapOpen;
	private static boolean pendingJoinSyncRequest;
	private static int ticksSinceJoin;
	private static long lastRequestMs;
	private static XaeroSyncPayload pendingPayload;

	private XaeroSyncClient() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;
		xaeroAvailable = isXaeroAvailable();
		DebugLog.info("Xaero sync client init. Xaero available: {}", xaeroAvailable);

		XaeroSyncPackets.registerPayloadTypes();

		ClientPlayNetworking.registerGlobalReceiver(XaeroSyncDataPayload.TYPE,
				(payload, context) -> context.client().execute(() -> {
					if (!xaeroAvailable) {
						return;
					}
					pendingPayload = payload.payload();
					DebugLog.info("Xaero sync payload received (warps: {}, homes: {}).", pendingPayload.warps().size(),
							pendingPayload.homes().size());
				}));

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (!xaeroAvailable) {
				return;
			}
			pendingJoinSyncRequest = true;
			ticksSinceJoin = 0;
			wasWorldMapOpen = false;
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			wasWorldMapOpen = false;
			pendingJoinSyncRequest = false;
			ticksSinceJoin = 0;
			lastRequestMs = 0L;
			pendingPayload = null;
		});

		ClientTickEvents.END_CLIENT_TICK.register(XaeroSyncClient::onClientTick);
	}

	private static void onClientTick(Minecraft client) {
		if (!xaeroAvailable || client == null) {
			return;
		}
		if (client.level == null) {
			return;
		}

		if (pendingJoinSyncRequest) {
			ticksSinceJoin++;
			if (ticksSinceJoin >= JOIN_REQUEST_DELAY_TICKS) {
				sendSyncRequest();
				pendingJoinSyncRequest = false;
			}
		}

		boolean worldMapOpen = client.screen != null
				&& WORLD_MAP_SCREEN_CLASS.equals(client.screen.getClass().getName());

		if (worldMapOpen && !wasWorldMapOpen) {
			sendSyncRequest();
		}

		if (pendingPayload != null) {
			try {
				if (XaeroCompat.applySyncPayload(pendingPayload)) {
					pendingPayload = null;
					DebugLog.info("Xaero sync payload applied.");
				}
			} catch (Throwable throwable) {
				ModConstants.LOGGER.error("Xaero sync apply failed; deferring retry.", throwable);
			}
		}

		wasWorldMapOpen = worldMapOpen;
	}

	private static void sendSyncRequest() {
		long now = Util.getMillis();
		if (now - lastRequestMs < REQUEST_COOLDOWN_MS) {
			return;
		}
		if (!ClientPlayNetworking.canSend(XaeroSyncRequestPayload.TYPE)) {
			return;
		}
		lastRequestMs = now;
		DebugLog.info("Sending Xaero sync request.");
		ClientPlayNetworking.send(new XaeroSyncRequestPayload());
	}

	private static boolean isXaeroAvailable() {
		FabricLoader loader = FabricLoader.getInstance();
		return loader.isModLoaded("xaerominimap") || loader.isModLoaded("xaeroworldmap");
	}
}
