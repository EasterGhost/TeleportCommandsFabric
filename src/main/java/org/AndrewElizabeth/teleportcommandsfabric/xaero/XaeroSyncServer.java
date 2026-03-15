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
	private static final Map<UUID, Long> LAST_REQUEST = new ConcurrentHashMap<>();
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
			LAST_REQUEST.remove(uuid);
		});

		ServerTickEvents.END_SERVER_TICK.register(XaeroSyncServer::onServerTick);
	}

	private static void handleSyncRequest(ServerPlayer player) {
		if (!isEnabled()) {
			return;
		}

		UUID uuid = player.getUUID();
		long now = System.currentTimeMillis();
		long lastRequest = LAST_REQUEST.getOrDefault(uuid, 0L);
		long requestIntervalMs = getRequestIntervalMs();
		if (now - lastRequest < requestIntervalMs) {
			Constants.LOGGER.debug("Xaero sync request throttled for {}", player.getName().getString());
			return;
		}

		LAST_REQUEST.put(uuid, now);
		XAERO_CLIENTS.add(uuid);
		sendSync(player);
		LAST_SYNC.put(uuid, now);
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
		Set<UUID> hiddenWarpUuids = StorageManager.STORAGE.getPlayer(player.getStringUUID())
				.map(playerData -> playerData.getHiddenWarpUuids())
				.orElse(Set.of());

		for (NamedLocation warp : StorageManager.STORAGE.getWarps()) {
			if (!warp.isXaeroVisible() || hiddenWarpUuids.contains(warp.getUuid())) {
				continue;
			}
			String worldId = warp.getWorldString();
			if (worldId == null || worldId.isBlank()) {
				continue;
			}
			warps.add(new XaeroSyncEntry(warp.getName(), worldId, warp.getX(), warp.getY(), warp.getZ()));
		}

		StorageManager.STORAGE.getPlayer(player.getStringUUID()).ifPresent(playerData -> {
			for (NamedLocation home : playerData.getHomes()) {
				if (!home.isXaeroVisible()) {
					continue;
				}
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

	private static long getRequestIntervalMs() {
		try {
			int configured = ConfigManager.CONFIG.getXaero().getSyncIntervalSeconds();
			if (configured > 0) {
				return configured * 1000L;
			}
		} catch (Exception e) {
			Constants.LOGGER.error("Xaero sync interval read error", e);
		}
		return Constants.SYNC_INTERVAL_MS;
	}
}
