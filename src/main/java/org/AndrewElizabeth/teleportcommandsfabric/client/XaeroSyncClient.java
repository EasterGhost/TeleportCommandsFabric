package org.AndrewElizabeth.teleportcommandsfabric.client;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.network.XaeroSyncDataPayload;
import org.AndrewElizabeth.teleportcommandsfabric.network.XaeroSyncPayload;
import org.AndrewElizabeth.teleportcommandsfabric.network.XaeroSyncPackets;
import org.AndrewElizabeth.teleportcommandsfabric.network.XaeroSyncRequestPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public final class XaeroSyncClient {
	private static final String WORLD_MAP_SCREEN_CLASS = "xaero.map.gui.GuiMap";
	private static final long REQUEST_COOLDOWN_MS = 5000L;
	private static boolean initialized;
	private static boolean xaeroAvailable;
	private static boolean wasWorldMapOpen;
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
		Constants.LOGGER.info("Xaero sync client init. Xaero available: {}", xaeroAvailable);

		XaeroSyncPackets.registerPayloadTypes();

		ClientPlayNetworking.registerGlobalReceiver(XaeroSyncDataPayload.TYPE,
				(payload, context) -> context.client().execute(() -> {
					if (!xaeroAvailable) {
						return;
					}
					pendingPayload = payload.payload();
					Constants.LOGGER.info("Xaero sync payload received (warps: {}, homes: {}).",
							pendingPayload.warps().size(),
							pendingPayload.homes().size());
					if (XaeroCompat.applySyncPayload(pendingPayload)) {
						pendingPayload = null;
						Constants.LOGGER.info("Xaero sync payload applied.");
					}
				}));

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			if (!xaeroAvailable) {
				return;
			}
			sendSyncRequest();
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			wasWorldMapOpen = false;
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

		boolean worldMapOpen = client.screen != null
				&& WORLD_MAP_SCREEN_CLASS.equals(client.screen.getClass().getName());

		if (worldMapOpen && !wasWorldMapOpen) {
			sendSyncRequest();
		}

		if (pendingPayload != null && XaeroCompat.applySyncPayload(pendingPayload)) {
			pendingPayload = null;
			Constants.LOGGER.info("Xaero sync payload applied after retry.");
		}

		wasWorldMapOpen = worldMapOpen;
	}

	private static void sendSyncRequest() {
		long now = System.currentTimeMillis();
		if (now - lastRequestMs < REQUEST_COOLDOWN_MS) {
			return;
		}
		lastRequestMs = now;
		Constants.LOGGER.info("Sending Xaero sync request.");
		ClientPlayNetworking.send(new XaeroSyncRequestPayload());
	}

	private static boolean isXaeroAvailable() {
		FabricLoader loader = FabricLoader.getInstance();
		return loader.isModLoaded("xaerominimap") || loader.isModLoaded("xaeroworldmap");
	}
}
