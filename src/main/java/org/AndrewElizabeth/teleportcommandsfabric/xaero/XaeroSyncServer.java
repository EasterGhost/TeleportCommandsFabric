package org.AndrewElizabeth.teleportcommandsfabric.xaero;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.network.XaeroSyncDataPayload;
import org.AndrewElizabeth.teleportcommandsfabric.network.XaeroSyncEntry;
import org.AndrewElizabeth.teleportcommandsfabric.network.XaeroSyncPackets;
import org.AndrewElizabeth.teleportcommandsfabric.network.XaeroSyncPayload;
import org.AndrewElizabeth.teleportcommandsfabric.network.XaeroSyncRequestPayload;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class XaeroSyncServer {
	private static final Set<UUID> XAERO_CLIENTS = ConcurrentHashMap.newKeySet();
	private static final Map<UUID, Long> LAST_SYNC = new ConcurrentHashMap<>();
	private static boolean initialized;

	private XaeroSyncServer() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;
		Constants.LOGGER.info("Xaero sync server hooks initializing.");

		XaeroSyncPackets.registerPayloadTypes();

		ServerPlayNetworking.registerGlobalReceiver(XaeroSyncRequestPayload.TYPE,
				(payload, context) -> handleSyncRequest(context.player()));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID uuid = handler.player.getUUID();
			XAERO_CLIENTS.remove(uuid);
			LAST_SYNC.remove(uuid);
		});

		ServerTickEvents.END_SERVER_TICK.register(XaeroSyncServer::onServerTick);
	}

	private static void handleSyncRequest(ServerPlayer player) {
		Constants.LOGGER.info("Xaero sync request from {}", player.getName().getString());
		if (!isEnabled()) {
			return;
		}

		XAERO_CLIENTS.add(player.getUUID());
		sendSync(player);
		LAST_SYNC.put(player.getUUID(), System.currentTimeMillis());
	}

	private static void onServerTick(MinecraftServer server) {
		if (!isEnabled()) {
			return;
		}

		int intervalSeconds = ConfigManager.CONFIG.getXaero().getSyncIntervalSeconds();
		if (intervalSeconds <= 0) {
			return;
		}

		long now = System.currentTimeMillis();
		long intervalMs = intervalSeconds * 1000L;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID uuid = player.getUUID();
			if (!XAERO_CLIENTS.contains(uuid)) {
				continue;
			}

			long last = LAST_SYNC.getOrDefault(uuid, 0L);
			if (now - last < intervalMs) {
				continue;
			}

			sendSync(player);
			LAST_SYNC.put(uuid, now);
		}
	}

	private static void sendSync(ServerPlayer player) {
		XaeroSyncPayload payload = buildPayload(player);
		Constants.LOGGER.info("Sending Xaero sync to {} (warps: {}, homes: {})",
				player.getName().getString(),
				payload.warps().size(),
				payload.homes().size());
		ServerPlayNetworking.send(player, new XaeroSyncDataPayload(payload));
	}

	private static XaeroSyncPayload buildPayload(ServerPlayer player) {
		List<XaeroSyncEntry> warps = new ArrayList<>();
		List<XaeroSyncEntry> homes = new ArrayList<>();

		for (NamedLocation warp : StorageManager.STORAGE.getWarps()) {
			String worldId = warp.getWorldString();
			if (worldId == null || worldId.isBlank()) {
				continue;
			}
			warps.add(new XaeroSyncEntry(warp.getName(), worldId, warp.getX(), warp.getY(), warp.getZ()));
		}

		StorageManager.STORAGE.getPlayer(player.getStringUUID()).ifPresent(playerData -> {
			for (NamedLocation home : playerData.getHomes()) {
				String worldId = home.getWorldString();
				if (worldId == null || worldId.isBlank()) {
					continue;
				}
				homes.add(new XaeroSyncEntry(home.getName(), worldId, home.getX(), home.getY(), home.getZ()));
			}
		});

		boolean persist = ConfigManager.CONFIG.getXaero().isPersistWaypointSets();
		String warpSetName = ConfigManager.CONFIG.getXaero().getWarpSetName();
		String homeSetName = ConfigManager.CONFIG.getXaero().getHomeSetName();
		return new XaeroSyncPayload(warps, homes, persist, warpSetName, homeSetName);
	}

	private static boolean isEnabled() {
		try {
			return ConfigManager.CONFIG.getXaero().isEnabled();
		} catch (Exception e) {
			Constants.LOGGER.error("Xaero sync config error", e);
			return false;
		}
	}
}
