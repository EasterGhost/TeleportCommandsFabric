package org.AndrewElizabeth.teleportcommandsfabric.integration.bluemap;

import de.bluecolored.bluemap.api.BlueMapAPI;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.config.RuntimeConfigHooks;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointMapSyncEvents;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TimeUtils;

import java.util.List;
import java.util.concurrent.CompletionException;

final class BlueMapWarpMarkerSync {
	private static volatile BlueMapAPI blueMapApi;
	private static volatile boolean enabled = true;
	private static volatile long syncIntervalMillis = TimeUtils.secondsToMillis(10);
	private static boolean initialized;
	private static boolean refreshInProgress;
	private static boolean markersApplied;
	private static long lastRefreshMillis;
	private static long changeVersion = 1L;
	private static long appliedVersion;

	private BlueMapWarpMarkerSync() {
	}

	static synchronized void initialize() {
		if (initialized) {
			return;
		}
		initialized = true;
		RuntimeConfigHooks.register(BlueMapWarpMarkerSync::applyConfig);
		WaypointMapSyncEvents.registerAllDirtyListener(BlueMapWarpMarkerSync::markDirty);
		ServerTickEvents.END_SERVER_TICK.register(BlueMapWarpMarkerSync::tick);
		BlueMapAPI.onEnable(BlueMapWarpMarkerSync::enable);
		BlueMapAPI.onDisable(BlueMapWarpMarkerSync::disable);
	}

	private static synchronized void applyConfig() {
		IntegrationRuntimeConfig config = ConfigManager.query(current -> new IntegrationRuntimeConfig(
				current.getIntegration().isEnabled(),
				TimeUtils.secondsToMillis(current.getIntegration().getSyncIntervalSeconds())));
		enabled = config.enabled();
		syncIntervalMillis = config.syncIntervalMillis();
		markDirty();
	}

	private static synchronized void enable(BlueMapAPI api) {
		blueMapApi = api;
		markDirty();
	}

	private static synchronized void disable(BlueMapAPI api) {
		if (blueMapApi == api) {
			blueMapApi = null;
		}
		markersApplied = false;
		refreshInProgress = false;
		markDirty();
	}

	private static synchronized void markDirty() {
		changeVersion++;
	}

	private static void tick(MinecraftServer server) {
		BlueMapAPI api = blueMapApi;
		if (api == null) {
			return;
		}
		if (!enabled) {
			clearMarkersIfNeeded(api);
			return;
		}
		long refreshVersion = beginRefresh();
		if (refreshVersion < 0L) {
			return;
		}
		lastRefreshMillis = Util.getMillis();
		GlobalProfileManager manager = TeleportCommands.GLOBAL_PROFILE_MANAGER;
		if (manager == null) {
			finishRefresh();
			return;
		}
		manager.query(profile -> List.copyOf(profile.getWarps()))
				.whenComplete((warps, throwable) -> server.execute(() -> applyQueryResult(api, refreshVersion, warps, throwable)));
	}

	private static synchronized long beginRefresh() {
		if (changeVersion == appliedVersion || refreshInProgress) {
			return -1L;
		}
		long now = Util.getMillis();
		if (now - lastRefreshMillis < syncIntervalMillis) {
			return -1L;
		}
		refreshInProgress = true;
		return changeVersion;
	}

	private static void applyQueryResult(BlueMapAPI api, long refreshVersion, List<NamedLocationView> warps, Throwable throwable) {
		try {
			if (throwable != null) {
				ModConstants.LOGGER.error("Failed to read warps for BlueMap marker sync.", unwrap(throwable));
				return;
			}
			if (blueMapApi != api || !enabled) {
				return;
			}
			BlueMapWarpMarkers.apply(api, warps);
			synchronized (BlueMapWarpMarkerSync.class) {
				if (changeVersion == refreshVersion) {
					appliedVersion = refreshVersion;
				}
				markersApplied = true;
			}
		} finally {
			finishRefresh();
		}
	}

	private static void clearMarkersIfNeeded(BlueMapAPI api) {
		boolean shouldClear;
		synchronized (BlueMapWarpMarkerSync.class) {
			shouldClear = markersApplied;
			markersApplied = false;
			appliedVersion = changeVersion;
			refreshInProgress = false;
		}
		if (shouldClear) {
			clearMarkers(api);
		}
	}

	private static void clearMarkers(BlueMapAPI api) {
		BlueMapWarpMarkers.clear(api);
	}

	private static synchronized void finishRefresh() {
		refreshInProgress = false;
	}

	private static Throwable unwrap(Throwable throwable) {
		if (throwable instanceof CompletionException && throwable.getCause() != null) {
			return throwable.getCause();
		}
		return throwable;
	}

	private record IntegrationRuntimeConfig(boolean enabled, long syncIntervalMillis) {
	}
}
