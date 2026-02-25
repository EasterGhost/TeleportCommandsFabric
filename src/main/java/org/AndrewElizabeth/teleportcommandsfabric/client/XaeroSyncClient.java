package org.AndrewElizabeth.teleportcommandsfabric.client;

import org.AndrewElizabeth.teleportcommandsfabric.network.XaeroSyncDataPayload;
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

	private XaeroSyncClient() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;
		xaeroAvailable = isXaeroAvailable();

		XaeroSyncPackets.registerPayloadTypes();

		ClientPlayNetworking.registerGlobalReceiver(XaeroSyncDataPayload.TYPE,
				(payload, context) -> context.client().execute(() -> {
					if (!xaeroAvailable) {
						return;
					}
					XaeroCompat.applySyncPayload(payload.payload());
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

		wasWorldMapOpen = worldMapOpen;
	}

	private static void sendSyncRequest() {
		long now = System.currentTimeMillis();
		if (now - lastRequestMs < REQUEST_COOLDOWN_MS) {
			return;
		}
		lastRequestMs = now;
		ClientPlayNetworking.send(new XaeroSyncRequestPayload());
	}

	private static boolean isXaeroAvailable() {
		FabricLoader loader = FabricLoader.getInstance();
		return loader.isModLoaded("xaerominimap") || loader.isModLoaded("xaeroworldmap");
	}
}
