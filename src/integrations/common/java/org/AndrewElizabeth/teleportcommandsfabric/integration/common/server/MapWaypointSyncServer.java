package org.AndrewElizabeth.teleportcommandsfabric.integration.common.server;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointMapSyncEvents;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.IntegrationProtocol;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.MapSyncPackets;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.legacy.LegacyXaeroSyncDataPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.legacy.LegacyXaeroSyncRequestPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol.ClientIntegrationHelloPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol.MapWaypointSnapshotPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedDeathLocation;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedMapWaypoint;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedWaypointKind;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocationView;
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
		ServerPlayNetworking.registerGlobalReceiver(LegacyXaeroSyncRequestPayload.TYPE,
				(payload, context) -> handleLegacyXaeroRequest(context.player()));
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
		CLIENTS.compute(player.getUUID(), (ignored, existing) -> {
			ClientState state = existing == null
					? new ClientState(SyncMode.COMMON, payload.protocolVersion())
					: existing;
			state.useCommon(payload.protocolVersion());
			state.markDirty();
			return state;
		});
	}

	private static void handleLegacyXaeroRequest(ServerPlayer player) {
		CLIENTS.compute(player.getUUID(), (ignored, existing) -> {
			if (existing != null && existing.syncMode() == SyncMode.COMMON) {
				return existing;
			}
			ClientState state = existing == null
					? new ClientState(SyncMode.LEGACY_XAERO, IntegrationProtocol.PROTOCOL_VERSION)
					: existing;
			state.useLegacyXaero();
			state.markDirty();
			return state;
		});
	}

	private static void onServerTick(MinecraftServer server) {
		if (!isEnabled()) {
			return;
		}

		long now = Util.getMillis();
		long wallNow = System.currentTimeMillis();
		long intervalMs = runtimeConfig.syncIntervalMillis();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ClientState state = CLIENTS.get(player.getUUID());
			if (state != null) {
				state.markDirtyIfHomeExpired(wallNow);
			}
			if (state == null || !state.shouldFlush(now, intervalMs)) {
				continue;
			}
			flush(server, player.getUUID(), state, now);
		}
	}

	private static void flush(MinecraftServer server, UUID playerUuid, ClientState state, long now) {
		state.beginFlush();
		SyncedDeathLocation deathLocation = deathLocation(playerUuid);
		buildSnapshot(playerUuid, deathLocation).whenComplete((result, throwable) -> {
			if (throwable != null) {
				server.execute(() -> failFlush(playerUuid, state, now, throwable));
				return;
			}
			server.execute(() -> sendIfChanged(server, playerUuid, state, result, now));
		});
	}

	private static void failFlush(UUID playerUuid, ClientState state, long now, Throwable throwable) {
		ModConstants.LOGGER.error("Failed to build map waypoint sync snapshot for {}", playerUuid, throwable);
		state.markDirty();
		state.finishFlush(now);
	}

	private static void sendIfChanged(MinecraftServer server, UUID playerUuid, ClientState state,
			SnapshotBuildResult result, long now) {
		try {
			MapWaypointSnapshot snapshot = result.snapshot();
			state.updateNextHomeExpiry(result.nextHomeExpiryMillis());
			ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
			if (player == null) {
				return;
			}
			if (!state.isSnapshotChanged(snapshot)) {
				return;
			}
			if (!sendSnapshot(player, state, snapshot)) {
				return;
			}
			state.updateSnapshot(snapshot);
			DebugLog.info("Sending {} map waypoint sync to {} (waypoints: {}).",
					state.syncMode().logName(), player.getName().getString(), snapshot.waypoints().size());
		} finally {
			state.finishFlush(now);
		}
	}

	private static boolean sendSnapshot(ServerPlayer player, ClientState state, MapWaypointSnapshot snapshot) {
		if (state.syncMode() == SyncMode.LEGACY_XAERO) {
			if (!ServerPlayNetworking.canSend(player, LegacyXaeroSyncDataPayload.TYPE)) {
				return false;
			}
			ServerPlayNetworking.send(player, new LegacyXaeroSyncDataPayload(snapshot));
			return true;
		}
		if (!ServerPlayNetworking.canSend(player, MapWaypointSnapshotPayload.TYPE)) {
			return false;
		}
		ServerPlayNetworking.send(player, new MapWaypointSnapshotPayload(state.protocolVersion(), snapshot));
		return true;
	}

	private static CompletableFuture<SnapshotBuildResult> buildSnapshot(UUID playerUuid,
			SyncedDeathLocation deathLocation) {
		GlobalProfileManager globalManager = TeleportCommands.GLOBAL_PROFILE_MANAGER;
		PlayerProfileManager playerManager = TeleportCommands.PLAYER_PROFILE_MANAGER;
		RuntimeConfig config = runtimeConfig;
		if (globalManager == null || playerManager == null) {
			return CompletableFuture.completedFuture(new SnapshotBuildResult(new MapWaypointSnapshot(List.of(),
					config.persistWaypointSets(), config.warpGroupName(), config.homeGroupName(), deathLocation), 0L));
		}

		CompletableFuture<List<NamedLocationView>> warpsFuture = globalManager.query(profile -> profile.getWarps());
		CompletableFuture<PlayerWaypointData> playerFuture = playerManager.query(playerUuid,
				profile -> new PlayerWaypointData(profile.getHomes(), profile.getHiddenWarpUuids()));

		return warpsFuture.thenCombine(playerFuture, (warps, playerData) -> {
			List<SyncedMapWaypoint> waypoints = new ArrayList<>();
			addWarps(waypoints, warps, playerData.hiddenWarpUuids());
			addHomes(waypoints, playerData.homes());
			MapWaypointSnapshot snapshot = new MapWaypointSnapshot(waypoints, config.persistWaypointSets(),
					config.warpGroupName(), config.homeGroupName(), deathLocation);
			return new SnapshotBuildResult(snapshot, nextHomeExpiryMillis(playerData.homes()));
		});
	}

	private static SyncedDeathLocation deathLocation(UUID playerUuid) {
		if (TeleportCommands.RECORDED_LOCATION_MANAGER == null) {
			return SyncedDeathLocation.NONE;
		}
		return TeleportCommands.RECORDED_LOCATION_MANAGER.getDeathLocation(playerUuid)
				.map(MapWaypointSyncServer::syncedDeathLocation)
				.orElse(SyncedDeathLocation.NONE);
	}

	private static SyncedDeathLocation syncedDeathLocation(RecordedLocationView location) {
		return new SyncedDeathLocation(location.getDimensionId(),
				location.getBlockPos().getX(), location.getBlockPos().getY(), location.getBlockPos().getZ());
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

	private static long nextHomeExpiryMillis(List<NamedLocationView> homes) {
		long nextExpiry = 0L;
		for (NamedLocationView home : homes) {
			if (!home.isVisible() || !home.isTemporary() || home.isExpired()) {
				continue;
			}
			long expiredTime = home.getExpiredTime();
			if (nextExpiry == 0L || expiredTime < nextExpiry) {
				nextExpiry = expiredTime;
			}
		}
		return nextExpiry;
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

	private record SnapshotBuildResult(MapWaypointSnapshot snapshot, long nextHomeExpiryMillis) {
	}

	private enum SyncMode {
		COMMON("common"),
		LEGACY_XAERO("legacy Xaero");

		private final String logName;

		SyncMode(String logName) {
			this.logName = logName;
		}

		private String logName() {
			return logName;
		}
	}

	private record RuntimeConfig(boolean enabled, long syncIntervalMillis, boolean persistWaypointSets,
			String warpGroupName, String homeGroupName) {
		private static RuntimeConfig defaults() {
			return new RuntimeConfig(true, ModConstants.SYNC_INTERVAL.toMillis(), true, "Default", "Default");
		}
	}

	private static final class ClientState {
		private volatile SyncMode syncMode;
		private volatile int protocolVersion;
		private volatile boolean dirty;
		private volatile boolean flushInProgress;
		private volatile long lastFlushTimeMillis;
		private volatile MapWaypointSnapshot lastSnapshot;
		private volatile long nextHomeExpiryMillis;

		private ClientState(SyncMode syncMode, int protocolVersion) {
			this.syncMode = syncMode;
			this.protocolVersion = protocolVersion;
		}

		private SyncMode syncMode() {
			return syncMode;
		}

		private int protocolVersion() {
			return protocolVersion;
		}

		private void useCommon(int protocolVersion) {
			this.syncMode = SyncMode.COMMON;
			this.protocolVersion = protocolVersion;
		}

		private void useLegacyXaero() {
			if (syncMode != SyncMode.COMMON) {
				syncMode = SyncMode.LEGACY_XAERO;
			}
		}

		private void markDirty() {
			dirty = true;
		}

		private void markDirtyIfHomeExpired(long nowMillis) {
			long expiryMillis = nextHomeExpiryMillis;
			if (expiryMillis <= 0L || nowMillis < expiryMillis) {
				return;
			}
			nextHomeExpiryMillis = 0L;
			markDirty();
		}

		private boolean shouldFlush(long now, long intervalMs) {
			return dirty && !flushInProgress && now - lastFlushTimeMillis >= intervalMs;
		}

		private void beginFlush() {
			flushInProgress = true;
			dirty = false;
		}

		private boolean isSnapshotChanged(MapWaypointSnapshot snapshot) {
			return !snapshot.equals(lastSnapshot);
		}

		private void updateSnapshot(MapWaypointSnapshot snapshot) {
			lastSnapshot = snapshot;
		}

		private void updateNextHomeExpiry(long expiryMillis) {
			nextHomeExpiryMillis = expiryMillis;
		}

		private void finishFlush(long now) {
			lastFlushTimeMillis = now;
			flushInProgress = false;
		}
	}
}
