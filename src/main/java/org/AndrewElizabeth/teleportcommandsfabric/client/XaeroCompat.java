package org.AndrewElizabeth.teleportcommandsfabric.client;

import org.AndrewElizabeth.teleportcommandsfabric.network.XaeroSyncEntry;
import org.AndrewElizabeth.teleportcommandsfabric.network.XaeroSyncPayload;
import org.AndrewElizabeth.teleportcommandsfabric.utils.tools;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.server.ServerWaypointManager;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.MinimapWorldManager;
import xaero.hud.minimap.world.container.MinimapWorldContainer;
import xaero.hud.minimap.world.container.MinimapWorldRootContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class XaeroCompat {
	private static final String WARP_PREFIX = "tpc:warp:";
	private static final String HOME_PREFIX = "tpc:home:";
	private static final String DEFAULT_SET_SENTINEL = "default";
	private static final String CURRENT_SET_SENTINEL = "current";
	private static final String LEGACY_WARP_SET_NAME = "TeleportCommands Warps";
	private static final String LEGACY_HOME_SET_NAME = "TeleportCommands Homes";
	private static final String PERSIST_WARP_PREFIX = "TPC-W ";
	private static final String PERSIST_HOME_PREFIX = "TPC-H ";
	private static final Map<String, Set<Integer>> WARP_SERVER_IDS = new HashMap<>();
	private static final Map<String, Set<Integer>> HOME_SERVER_IDS = new HashMap<>();

	private XaeroCompat() {
	}

	@SuppressWarnings("deprecation")
	public static boolean applySyncPayload(XaeroSyncPayload payload) {
		XaeroMinimapSession session = XaeroMinimapSession.getCurrentSession();
		if (session == null || session.getMinimapProcessor() == null) {
			return false;
		}

		MinimapSession minimapSession = session.getMinimapProcessor().getSession();
		if (minimapSession == null) {
			return false;
		}

		MinimapWorldManager worldManager = minimapSession.getWorldManager();
		if (worldManager == null) {
			return false;
		}

		boolean persist = payload.persistWaypointSets();
		String warpSetName = payload.warpSetName();
		String homeSetName = payload.homeSetName();

		applyEntries(worldManager, payload.warps(), EntryType.WARP, persist, warpSetName, homeSetName);
		applyEntries(worldManager, payload.homes(), EntryType.HOME, persist, warpSetName, homeSetName);
		return true;
	}

	private static void applyEntries(MinimapWorldManager worldManager, List<XaeroSyncEntry> entries,
			EntryType type, boolean persist, String warpSetName, String homeSetName) {
		Map<String, List<XaeroSyncEntry>> byWorld = groupByWorld(entries);
		for (Map.Entry<String, List<XaeroSyncEntry>> entry : byWorld.entrySet()) {
			String worldId = entry.getKey();
			MinimapWorld world = findWorld(worldManager, worldId);
			if (world == null) {
				continue;
			}

			List<Waypoint> waypoints = toWaypoints(entry.getValue(), type);
			updateServerWaypoints(world.getContainer(), worldId, waypoints, type);
			persistWaypoints(world, entry.getValue(), waypoints, type, persist, warpSetName, homeSetName);
		}

			clearMissingWorlds(worldManager, byWorld.keySet(), type, persist, warpSetName, homeSetName);
	}

	private static void clearMissingWorlds(MinimapWorldManager worldManager, Set<String> activeWorlds,
			EntryType type, boolean persist, String warpSetName, String homeSetName) {
		Map<String, Set<Integer>> idsByWorld = type == EntryType.WARP ? WARP_SERVER_IDS : HOME_SERVER_IDS;
		Set<String> previousWorlds = new HashSet<>(idsByWorld.keySet());
		previousWorlds.removeAll(activeWorlds);
		for (String worldId : previousWorlds) {
			MinimapWorld world = findWorld(worldManager, worldId);
			if (world == null) {
				idsByWorld.remove(worldId);
				continue;
			}
			updateServerWaypoints(world.getContainer(), worldId, List.of(), type);
			persistWaypoints(world, List.of(), List.of(), type, persist, warpSetName, homeSetName);
		}
	}

	private static Map<String, List<XaeroSyncEntry>> groupByWorld(List<XaeroSyncEntry> entries) {
		Map<String, List<XaeroSyncEntry>> result = new HashMap<>();
		for (XaeroSyncEntry entry : entries) {
			result.computeIfAbsent(entry.worldId(), key -> new ArrayList<>()).add(entry);
		}
		return result;
	}

	private static MinimapWorld findWorld(MinimapWorldManager worldManager, String worldId) {
		for (MinimapWorldRootContainer root : worldManager.getRootContainers()) {
			for (MinimapWorld world : root.getAllWorldsIterable()) {
				String currentId = tools.getDimensionId(world.getDimId());
				if (worldId.equals(currentId)) {
					return world;
				}
			}
		}
		return null;
	}

	private static List<Waypoint> toWaypoints(List<XaeroSyncEntry> entries, EntryType type) {
		List<Waypoint> waypoints = new ArrayList<>(entries.size());
		WaypointColor color = type == EntryType.WARP ? WaypointColor.BLUE : WaypointColor.GREEN;
		String symbol = type == EntryType.WARP ? "W" : "H";

		for (XaeroSyncEntry entry : entries) {
			Waypoint waypoint = new Waypoint(
					entry.x(),
					entry.y(),
					entry.z(),
					entry.name(),
					symbol,
					color,
					WaypointPurpose.NORMAL);
			waypoint.setYIncluded(true);
			waypoints.add(waypoint);
		}
		return waypoints;
	}

	private static List<Waypoint> toTaggedWaypoints(List<XaeroSyncEntry> entries, EntryType type) {
		List<Waypoint> waypoints = new ArrayList<>(entries.size());
		WaypointColor color = type == EntryType.WARP ? WaypointColor.BLUE : WaypointColor.GREEN;
		String symbol = type == EntryType.WARP ? "W" : "H";
		String prefix = type == EntryType.WARP ? PERSIST_WARP_PREFIX : PERSIST_HOME_PREFIX;

		for (XaeroSyncEntry entry : entries) {
			Waypoint waypoint = new Waypoint(
					entry.x(),
					entry.y(),
					entry.z(),
					prefix + entry.name(),
					symbol,
					color,
					WaypointPurpose.NORMAL);
			waypoint.setYIncluded(true);
			waypoints.add(waypoint);
		}
		return waypoints;
	}

	private static void updateServerWaypoints(MinimapWorldContainer container, String worldId,
			List<Waypoint> waypoints, EntryType type) {
		if (container == null) {
			return;
		}

		ServerWaypointManager manager = container.getServerWaypointManager();
		if (manager == null) {
			return;
		}

		Map<String, Set<Integer>> idsByWorld = type == EntryType.WARP ? WARP_SERVER_IDS : HOME_SERVER_IDS;
		Set<Integer> previous = idsByWorld.getOrDefault(worldId, new HashSet<>());
		Set<Integer> next = new HashSet<>();

		String prefix = type == EntryType.WARP ? WARP_PREFIX : HOME_PREFIX;
		for (Waypoint waypoint : waypoints) {
			int id = computeId(prefix, worldId, waypoint.getName());
			manager.remove(id);
			manager.add(id, waypoint);
			next.add(id);
		}

		for (Integer oldId : previous) {
			if (!next.contains(oldId)) {
				manager.remove(oldId);
			}
		}

		idsByWorld.put(worldId, next);
	}

	private static void persistWaypoints(MinimapWorld world, List<XaeroSyncEntry> entries, List<Waypoint> waypoints,
			EntryType type, boolean persist, String warpSetName, String homeSetName) {
		if (!persist) {
			return;
		}

		String setName = type == EntryType.WARP ? warpSetName : homeSetName;
		boolean useDefaultSet = isDefaultSet(setName);
		WaypointSet set = useDefaultSet ? world.getCurrentWaypointSet() : world.getWaypointSet(setName);
		if (set == null && !useDefaultSet) {
			world.addWaypointSet(setName);
			set = world.getWaypointSet(setName);
		}
		if (set == null) {
			return;
		}

		if (useDefaultSet) {
			removeTaggedWaypoints(set, type);
			set.addAll(toTaggedWaypoints(entries, type));
			return;
		}

		set.clear();
		set.addAll(waypoints);
	}

	private static void removeTaggedWaypoints(WaypointSet set, EntryType type) {
		String prefix = type == EntryType.WARP ? PERSIST_WARP_PREFIX : PERSIST_HOME_PREFIX;
		List<Waypoint> toRemove = new ArrayList<>();
		for (Waypoint waypoint : set.getWaypoints()) {
			String name = waypoint.getName();
			if (name != null && name.startsWith(prefix)) {
				toRemove.add(waypoint);
			}
		}
		if (!toRemove.isEmpty()) {
			set.removeAll(toRemove);
		}
	}

	private static boolean isDefaultSet(String setName) {
		if (setName == null || setName.isBlank()) {
			return true;
		}
		String normalized = setName.trim().toLowerCase();
		return DEFAULT_SET_SENTINEL.equals(normalized)
				|| CURRENT_SET_SENTINEL.equals(normalized)
				|| LEGACY_WARP_SET_NAME.equals(setName)
				|| LEGACY_HOME_SET_NAME.equals(setName);
	}

	private static int computeId(String prefix, String worldId, String name) {
		String key = prefix + worldId + ":" + name;
		return 0x54000000 ^ key.hashCode();
	}


	private enum EntryType {
		WARP,
		HOME
	}
}
