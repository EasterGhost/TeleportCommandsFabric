package org.AndrewElizabeth.teleportcommandsfabric.integration.xaero;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.config.section.XaeroConfig;
import org.AndrewElizabeth.teleportcommandsfabric.network.core.XaeroSyncPackets;
import org.AndrewElizabeth.teleportcommandsfabric.network.protocol.xaero.XaeroSyncDataPayload;
import org.AndrewElizabeth.teleportcommandsfabric.network.protocol.xaero.XaeroSyncEntry;
import org.AndrewElizabeth.teleportcommandsfabric.network.protocol.xaero.XaeroSyncPayload;
import org.AndrewElizabeth.teleportcommandsfabric.network.protocol.xaero.XaeroSyncRequestPayload;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class XaeroSyncServer {
	private static final Set<UUID> XAERO_CLIENTS = ConcurrentHashMap.newKeySet();
	private static final ConcurrentMap<UUID, Long> LAST_SYNC = new ConcurrentHashMap<>();
	private static final ConcurrentMap<UUID, Long> LAST_REQUEST = new ConcurrentHashMap<>();
	private static boolean initialized;

	private XaeroSyncServer() {
	}

	public static synchronized void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;
		ModConstants.LOGGER.info("Xaero sync server hooks initializing.");

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
			ModConstants.LOGGER.debug("Xaero sync request throttled for {}", player.getName().getString());
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

		int intervalSeconds = getSyncIntervalSeconds();
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
		UUID playerUuid = player.getUUID();
		MinecraftServer server = player.level().getServer();
		buildPayload(playerUuid).whenComplete((payload, throwable) -> {
			if (throwable != null) {
				ModConstants.LOGGER.error("Failed to build Xaero sync payload for {}", playerUuid, throwable);
				return;
			}
			server.execute(() -> sendBuiltPayload(server, playerUuid, payload));
		});
	}

	private static void sendBuiltPayload(MinecraftServer server, UUID playerUuid, XaeroSyncPayload payload) {
		ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
		if (player == null || !ServerPlayNetworking.canSend(player, XaeroSyncDataPayload.TYPE)) {
			return;
		}

		ModConstants.LOGGER.info("Sending Xaero sync to {} (warps: {}, homes: {})",
				player.getName().getString(), payload.warps().size(), payload.homes().size());
		ServerPlayNetworking.send(player, new XaeroSyncDataPayload(payload));
	}

	private static CompletableFuture<XaeroSyncPayload> buildPayload(UUID playerUuid) {
		GlobalProfileManager globalManager = TeleportCommands.GLOBAL_PROFILE_MANAGER;
		PlayerProfileManager playerManager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		if (globalManager == null || playerManager == null) {
			return CompletableFuture.completedFuture(emptyPayload());
		}

		XaeroSnapshotConfig config = readSnapshotConfig();
		CompletableFuture<List<NamedLocationView>> warpsFuture = globalManager.query(profile -> profile.getWarps());
		CompletableFuture<PlayerXaeroData> playerFuture = playerManager.query(playerUuid,
				profile -> new PlayerXaeroData(profile.getHomes(), profile.getHiddenWarpUuids()));

		return warpsFuture.thenCombine(playerFuture, (warps, playerData) -> {
			List<XaeroSyncEntry> warpEntries = toWarpEntries(warps, playerData.hiddenWarpUuids());
			List<XaeroSyncEntry> homeEntries = toHomeEntries(playerData.homes());
			return new XaeroSyncPayload(warpEntries, homeEntries, config.persistWaypointSets(),
					config.warpSetName(), config.homeSetName());
		});
	}

	private static List<XaeroSyncEntry> toWarpEntries(List<NamedLocationView> warps, Set<UUID> hiddenWarpUuids) {
		List<XaeroSyncEntry> entries = new ArrayList<>();
		for (NamedLocationView warp : warps) {
			if (!warp.isVisible() || hiddenWarpUuids.contains(warp.getUuid())) {
				continue;
			}
			addEntry(entries, warp);
		}
		return entries;
	}

	private static List<XaeroSyncEntry> toHomeEntries(List<NamedLocationView> homes) {
		List<XaeroSyncEntry> entries = new ArrayList<>();
		for (NamedLocationView home : homes) {
			if (!home.isVisible() || home.isExpired()) {
				continue;
			}
			addEntry(entries, home);
		}
		return entries;
	}

	private static void addEntry(List<XaeroSyncEntry> entries, NamedLocationView location) {
		String worldId = location.getDimensionId();
		if (worldId == null || worldId.isBlank()) {
			return;
		}
		entries.add(new XaeroSyncEntry(location.getName(), worldId, location.getX(), location.getY(), location.getZ()));
	}

	private static XaeroSyncPayload emptyPayload() {
		XaeroSnapshotConfig config = readSnapshotConfig();
		return new XaeroSyncPayload(List.of(), List.of(), config.persistWaypointSets(),
				config.warpSetName(), config.homeSetName());
	}

	private static boolean isEnabled() {
		try {
			return ConfigManager.query(config -> config.getXaero().isEnabled());
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Xaero sync config error", exception);
			return false;
		}
	}

	private static int getSyncIntervalSeconds() {
		try {
			return ConfigManager.query(config -> config.getXaero().getSyncIntervalSeconds());
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Xaero sync interval read error", exception);
			return (int) ModConstants.SYNC_INTERVAL.toSeconds();
		}
	}

	private static long getRequestIntervalMs() {
		int configured = getSyncIntervalSeconds();
		if (configured > 0) {
			return configured * 1000L;
		}
		return ModConstants.SYNC_INTERVAL.toMillis();
	}

	private static XaeroSnapshotConfig readSnapshotConfig() {
		try {
			return ConfigManager.query(config -> {
				XaeroConfig xaero = config.getXaero();
				return new XaeroSnapshotConfig(xaero.isPersistWaypointSets(), xaero.getWarpSetName(),
						xaero.getHomeSetName());
			});
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Xaero sync config read error", exception);
			return new XaeroSnapshotConfig(true, "Default", "Default");
		}
	}

	private record PlayerXaeroData(List<NamedLocationView> homes, Set<UUID> hiddenWarpUuids) {
	}

	private record XaeroSnapshotConfig(boolean persistWaypointSets, String warpSetName, String homeSetName) {
	}
}
