package org.AndrewElizabeth.teleportcommandsfabric.integration.common.server;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointMapSyncEvents;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.IntegrationProtocol;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.MapSyncPackets;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.legacy.LegacyXaeroSyncDataPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.legacy.LegacyXaeroSyncRequestPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol.ClientIntegrationHelloPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.network.protocol.MapWaypointSnapshotPayload;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedDeathLocation;
import org.AndrewElizabeth.teleportcommandsfabric.utils.DebugLog;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TimeUtils;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class MapWaypointSyncServer {
	private static final ConcurrentMap<UUID, MapWaypointClientState> CLIENTS = new ConcurrentHashMap<>();
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
		MapWaypointClientState state = CLIENTS.get(playerUuid);
		if (state != null) {
			state.markDirty();
		}
	}

	public static void markAllDirty() {
		for (MapWaypointClientState state : CLIENTS.values()) {
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
			MapWaypointClientState state = existing == null
					? new MapWaypointClientState(MapWaypointSyncMode.COMMON, payload.protocolVersion())
					: existing;
			state.useCommon(payload.protocolVersion());
			state.markDirty();
			return state;
		});
	}

	private static void handleLegacyXaeroRequest(ServerPlayer player) {
		CLIENTS.compute(player.getUUID(), (ignored, existing) -> {
			if (existing != null && existing.syncMode() == MapWaypointSyncMode.COMMON) {
				return existing;
			}
			MapWaypointClientState state = existing == null
					? new MapWaypointClientState(MapWaypointSyncMode.LEGACY_XAERO,
							IntegrationProtocol.PROTOCOL_VERSION)
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
			MapWaypointClientState state = CLIENTS.get(player.getUUID());
			if (state != null) {
				state.markDirtyIfHomeExpired(wallNow);
			}
			if (state == null || !state.shouldFlush(now, intervalMs)) {
				continue;
			}
			flush(server, player.getUUID(), state, now);
		}
	}

	private static void flush(MinecraftServer server, UUID playerUuid, MapWaypointClientState state, long now) {
		state.beginFlush();
		SyncedDeathLocation deathLocation = MapWaypointSnapshotBuilder.deathLocation(playerUuid);
		MapWaypointSnapshotBuilder.build(playerUuid, deathLocation, snapshotOptions()).whenComplete((result, throwable) -> {
			if (throwable != null) {
				server.execute(() -> failFlush(playerUuid, state, now, throwable));
				return;
			}
			server.execute(() -> sendIfChanged(server, playerUuid, state, result, now));
		});
	}

	private static void failFlush(UUID playerUuid, MapWaypointClientState state, long now, Throwable throwable) {
		ModConstants.LOGGER.error("Failed to build map waypoint sync snapshot for {}", playerUuid, throwable);
		state.markDirty();
		state.finishFlush(now);
	}

	private static void sendIfChanged(MinecraftServer server, UUID playerUuid, MapWaypointClientState state,
			MapWaypointSnapshotBuilder.Result result, long now) {
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

	private static boolean sendSnapshot(ServerPlayer player, MapWaypointClientState state, MapWaypointSnapshot snapshot) {
		if (state.syncMode() == MapWaypointSyncMode.LEGACY_XAERO) {
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

	private static boolean isEnabled() {
		return runtimeConfig.enabled();
	}

	private static MapWaypointSnapshotBuilder.Options snapshotOptions() {
		RuntimeConfig config = runtimeConfig;
		return new MapWaypointSnapshotBuilder.Options(config.persistWaypointSets(), config.warpGroupName(), config.homeGroupName());
	}

	private record RuntimeConfig(boolean enabled, long syncIntervalMillis, boolean persistWaypointSets,
			String warpGroupName, String homeGroupName) {
		private static RuntimeConfig defaults() {
			return new RuntimeConfig(true, ModConstants.SYNC_INTERVAL.toMillis(), true, "Default", "Default");
		}
	}
}
