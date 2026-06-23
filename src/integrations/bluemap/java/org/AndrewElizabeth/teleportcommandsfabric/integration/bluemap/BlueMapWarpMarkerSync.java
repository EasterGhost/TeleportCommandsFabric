package org.AndrewElizabeth.teleportcommandsfabric.integration.bluemap;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;

final class BlueMapWarpMarkerSync {
	private static final String MARKER_SET_ID = "teleport_commands_fabric_warps";
	private static final String MARKER_SET_LABEL = "Warps";

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
			applyMarkers(api, warps);
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

	private static void applyMarkers(BlueMapAPI api, List<NamedLocationView> warps) {
		clearMarkers(api);
		Map<BlueMapWorld, MarkerSet> markerSets = new HashMap<>();
		for (NamedLocationView warp : warps) {
			world(api, warp).ifPresent(world -> markerSets
					.computeIfAbsent(world, ignored -> MarkerSet.builder()
							.label(MARKER_SET_LABEL)
							.toggleable(true)
							.defaultHidden(false)
							.build())
					.put(warp.getUuid().toString(), marker(warp)));
		}
		for (Map.Entry<BlueMapWorld, MarkerSet> entry : markerSets.entrySet()) {
			for (BlueMapMap map : entry.getKey().getMaps()) {
				map.getMarkerSets().put(MARKER_SET_ID, entry.getValue());
			}
		}
	}

	private static Optional<BlueMapWorld> world(BlueMapAPI api, NamedLocationView warp) {
		Optional<BlueMapWorld> world = api.getWorld(warp.getDimension());
		if (world.isPresent()) {
			return world;
		}
		return api.getWorld(warp.getDimensionId());
	}

	private static POIMarker marker(NamedLocationView warp) {
		String detail = "Warp: " + htmlEscape(warp.getName()) + "<br>"
				+ "Dimension: " + htmlEscape(warp.getDimensionId()) + "<br>"
				+ "Location: " + warp.getX() + ", " + formatY(warp.getYPrecise()) + ", " + warp.getZ();
		return POIMarker.builder()
				.label(warp.getName())
				.position(new Vector3d(warp.getX() + 0.5D, warp.getYPrecise(), warp.getZ() + 0.5D))
				.detail(detail)
				.build();
	}

	private static String formatY(double value) {
		if (value == Math.rint(value)) {
			return Integer.toString((int) value);
		}
		return Double.toString(value);
	}

	private static String htmlEscape(String value) {
		return value.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
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
		for (BlueMapMap map : api.getMaps()) {
			map.getMarkerSets().remove(MARKER_SET_ID);
		}
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
