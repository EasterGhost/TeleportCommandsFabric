package org.AndrewElizabeth.teleportcommandsfabric.integration.common.server;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointMapSyncEvents;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.IntegrationProtocol;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.MapSyncPackets;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol.ClientIntegrationHelloPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol.MapWaypointSnapshotPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedMapWaypoint;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedWaypointKind;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.utils.DebugLog;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TimeUtils;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class MapWaypointSyncServer {
	private static final ConcurrentMap<UUID, ClientState> CLIENTS = new ConcurrentHashMap<>();
	private static volatile RuntimeConfig runtimeConfig = RuntimeConfig.defaults();
	private static boolean initialized;

	private MapWaypointSyncServer() {
	}

	public static void applyConfig(boolean enabled, int syncIntervalSeconds, boolean persistWaypointSets,
			String warpGroupName, String homeGroupName) {
		runtimeConfig = new RuntimeConfig(enabled, TimeUtils.secondsToMillis(syncIntervalSeconds),
				persistWaypointSets, warpGroupName, homeGroupName);
		markAllDirty();
	}

	public static synchronized void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;
		DebugLog.info("Integration common sync server hooks initializing.");

		MapSyncPackets.registerPayloadTypes();
		ServerPlayNetworking.registerGlobalReceiver(ClientIntegrationHelloPayload.TYPE,
				(payload, context) -> handleHello(context.player(), payload));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CLIENTS.remove(handler.player.getUUID()));
		ServerTickEvents.END_SERVER_TICK.register(MapWaypointSyncServer::onServerTick);

		WaypointMapSyncEvents.registerPlayerDirtyListener(MapWaypointSyncServer::markDirty);
		WaypointMapSyncEvents.registerAllDirtyListener(MapWaypointSyncServer::markAllDirty);
	}

	public static void markDirty(UUID playerUuid) {
		ClientState state = CLIENTS.get(playerUuid);
		if (state != null) {
			state.markDirty();
		}
	}

	public static void markAllDirty() {
		for (ClientState state : CLIENTS.values()) {
			state.markDirty();
		}
	}

	private static void handleHello(ServerPlayer player, ClientIntegrationHelloPayload payload) {
		if (!IntegrationProtocol.isSupported(payload.protocolVersion())) {
			DebugLog.debug("Ignoring unsupported integration protocol {} from {}.",
					payload.protocolVersion(), player.getName().getString());
			return;
		}
		ClientState state = new ClientState(payload.protocolVersion());
		state.markDirty();
		CLIENTS.put(player.getUUID(), state);
	}

	private static void onServerTick(MinecraftServer server) {
		if (!isEnabled()) {
			return;
		}

		long now = Util.getMillis();
		long intervalMs = runtimeConfig.syncIntervalMillis();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ClientState state = CLIENTS.get(player.getUUID());
			if (state == null || !state.shouldFlush(now, intervalMs)) {
				continue;
			}
			flush(server, player.getUUID(), state, now);
		}
	}

	private static void flush(MinecraftServer server, UUID playerUuid, ClientState state, long now) {
		state.beginFlush();
		buildSnapshot(playerUuid).whenComplete((snapshot, throwable) -> {
			if (throwable != null) {
				ModConstants.LOGGER.error("Failed to build map waypoint sync snapshot for {}", playerUuid, throwable);
				state.markDirty();
				state.finishFlush(now);
				return;
			}
			server.execute(() -> sendIfChanged(server, playerUuid, state, snapshot, now));
		});
	}

	private static void sendIfChanged(MinecraftServer server, UUID playerUuid, ClientState state,
			MapWaypointSnapshot snapshot, long now) {
		try {
			ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
			if (player == null || !ServerPlayNetworking.canSend(player, MapWaypointSnapshotPayload.TYPE)) {
				return;
			}
			int snapshotHash = snapshot.hashCode();
			if (!state.isSnapshotChanged(snapshotHash)) {
				return;
			}
			ServerPlayNetworking.send(player, new MapWaypointSnapshotPayload(state.protocolVersion(), snapshot));
			state.updateSnapshotHash(snapshotHash);
			DebugLog.info("Sending map waypoint sync to {} (waypoints: {}).",
					player.getName().getString(), snapshot.waypoints().size());
		} finally {
			state.finishFlush(now);
		}
	}

	private static CompletableFuture<MapWaypointSnapshot> buildSnapshot(UUID playerUuid) {
		GlobalProfileManager globalManager = TeleportCommands.GLOBAL_PROFILE_MANAGER;
		PlayerProfileManager playerManager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		RuntimeConfig config = runtimeConfig;
		if (globalManager == null || playerManager == null) {
			return CompletableFuture.completedFuture(new MapWaypointSnapshot(List.of(),
					config.persistWaypointSets(), config.warpGroupName(), config.homeGroupName()));
		}

		CompletableFuture<List<NamedLocationView>> warpsFuture = globalManager.query(profile -> profile.getWarps());
		CompletableFuture<PlayerWaypointData> playerFuture = playerManager.query(playerUuid,
				profile -> new PlayerWaypointData(profile.getHomes(), profile.getHiddenWarpUuids()));

		return warpsFuture.thenCombine(playerFuture, (warps, playerData) -> {
			List<SyncedMapWaypoint> waypoints = new ArrayList<>();
			addWarps(waypoints, warps, playerData.hiddenWarpUuids());
			addHomes(waypoints, playerData.homes());
			return new MapWaypointSnapshot(waypoints, config.persistWaypointSets(), config.warpGroupName(), config.homeGroupName());
		});
	}

	private static void addWarps(List<SyncedMapWaypoint> waypoints, List<NamedLocationView> warps, Set<UUID> hiddenWarpUuids) {
		for (NamedLocationView warp : warps) {
			if (!warp.isVisible() || hiddenWarpUuids.contains(warp.getUuid())) {
				continue;
			}
			addWaypoint(waypoints, SyncedWaypointKind.WARP, warp);
		}
	}

	private static void addHomes(List<SyncedMapWaypoint> waypoints, List<NamedLocationView> homes) {
		for (NamedLocationView home : homes) {
			if (!home.isVisible() || home.isExpired()) {
				continue;
			}
			addWaypoint(waypoints, SyncedWaypointKind.HOME, home);
		}
	}

	private static void addWaypoint(List<SyncedMapWaypoint> waypoints, SyncedWaypointKind kind, NamedLocationView location) {
		String worldId = location.getDimensionId();
		if (worldId == null || worldId.isBlank()) {
			return;
		}
		waypoints.add(new SyncedMapWaypoint(kind, location.getName(), worldId,
				location.getX(), location.getY(), location.getZ()));
	}

	private static boolean isEnabled() {
		return runtimeConfig.enabled();
	}

	private record PlayerWaypointData(List<NamedLocationView> homes, Set<UUID> hiddenWarpUuids) {
	}

	private record RuntimeConfig(boolean enabled, long syncIntervalMillis, boolean persistWaypointSets,
			String warpGroupName, String homeGroupName) {
		private static RuntimeConfig defaults() {
			return new RuntimeConfig(true, ModConstants.SYNC_INTERVAL.toMillis(), true, "Default", "Default");
		}
	}

	private static final class ClientState {
		private final int protocolVersion;
		private volatile boolean dirty;
		private volatile boolean flushInProgress;
		private volatile long lastFlushTimeMillis;
		private volatile int lastSnapshotHash;
		private volatile boolean hasSnapshotHash;

		private ClientState(int protocolVersion) {
			this.protocolVersion = protocolVersion;
		}

		private int protocolVersion() {
			return protocolVersion;
		}

		private void markDirty() {
			dirty = true;
		}

		private boolean shouldFlush(long now, long intervalMs) {
			return dirty && !flushInProgress && now - lastFlushTimeMillis >= intervalMs;
		}

		private void beginFlush() {
			flushInProgress = true;
			dirty = false;
		}

		private boolean isSnapshotChanged(int snapshotHash) {
			return !hasSnapshotHash || lastSnapshotHash != snapshotHash;
		}

		private void updateSnapshotHash(int snapshotHash) {
			lastSnapshotHash = snapshotHash;
			hasSnapshotHash = true;
		}

		private void finishFlush(long now) {
			lastFlushTimeMillis = now;
			flushInProgress = false;
		}
	}
}
