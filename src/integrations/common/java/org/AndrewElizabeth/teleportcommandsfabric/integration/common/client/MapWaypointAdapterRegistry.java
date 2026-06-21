package org.AndrewElizabeth.teleportcommandsfabric.integration.common.client;

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
		if (!adapter.applySnapshot(ClientMapWaypointSnapshots.latest())) {
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
			adapter.clear();
		}
	}

	static void dispatch(MapWaypointSnapshot snapshot) {
		boolean allApplied = true;
		for (MapWaypointAdapter adapter : ADAPTERS.values()) {
			if (!adapter.applySnapshot(snapshot)) {
				allApplied = false;
			}
		}
		pendingDispatch = !allApplied;
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
			adapter.clear();
		}
		pendingDispatch = false;
	}

	static boolean hasAdapters() {
		return !ADAPTERS.isEmpty();
	}
}
