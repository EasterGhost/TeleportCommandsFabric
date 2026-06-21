package org.AndrewElizabeth.teleportcommandsfabric.integration.common.client;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MapWaypointAdapterRegistry {
	private static final Map<String, MapWaypointAdapter> ADAPTERS = new ConcurrentHashMap<>();
	private static volatile boolean pendingDispatch;

	private MapWaypointAdapterRegistry() {
	}

	public static void register(MapWaypointAdapter adapter) {
		if (adapter == null) {
			return;
		}
		ADAPTERS.put(adapter.id(), adapter);
		if (!applySnapshot(adapter, ClientMapWaypointSnapshots.latest())) {
			pendingDispatch = true;
		}
		MapWaypointSyncClient.requestHello();
	}

	public static void unregister(String id) {
		if (id == null) {
			return;
		}
		MapWaypointAdapter adapter = ADAPTERS.remove(id);
		if (adapter != null) {
			clearAdapter(adapter);
		}
	}

	static void dispatch(MapWaypointSnapshot snapshot) {
		boolean allApplied = true;
		for (MapWaypointAdapter adapter : ADAPTERS.values()) {
			if (!applySnapshot(adapter, snapshot)) {
				allApplied = false;
			}
		}
		pendingDispatch = !allApplied;
	}

	private static boolean applySnapshot(MapWaypointAdapter adapter, MapWaypointSnapshot snapshot) {
		try {
			return adapter.applySnapshot(snapshot);
		} catch (RuntimeException exception) {
			ModConstants.LOGGER.error("Failed to apply map waypoint snapshot with {} adapter.", adapter.id(), exception);
			return false;
		}
	}

	private static void clearAdapter(MapWaypointAdapter adapter) {
		try {
			adapter.clear();
		} catch (RuntimeException exception) {
			ModConstants.LOGGER.error("Failed to clear map waypoint snapshot with {} adapter.", adapter.id(), exception);
		}
	}

	static void retryPending() {
		if (!pendingDispatch) {
			return;
		}
		dispatch(ClientMapWaypointSnapshots.latest());
	}

	static void clearAll() {
		ClientMapWaypointSnapshots.clear();
		for (MapWaypointAdapter adapter : ADAPTERS.values()) {
			clearAdapter(adapter);
		}
		pendingDispatch = false;
	}

	static boolean hasAdapters() {
		return !ADAPTERS.isEmpty();
	}
}
