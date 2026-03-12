package org.AndrewElizabeth.teleportcommandsfabric.client;

import org.AndrewElizabeth.teleportcommandsfabric.network.XaeroSyncEntry;
import org.AndrewElizabeth.teleportcommandsfabric.network.XaeroSyncPayload;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.MinimapWorldManager;
import xaero.hud.minimap.world.container.MinimapWorldRootContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class XaeroCompat {
	private static final String DEFAULT_SET_SENTINEL = "default";
	private static final String CURRENT_SET_SENTINEL = "current";
	private static final String LEGACY_WARP_SET_NAME = "TeleportCommands Warps";
	private static final String LEGACY_HOME_SET_NAME = "TeleportCommands Homes";
	private static final String PERSIST_WARP_PREFIX = "TPC-W ";
	private static final String PERSIST_HOME_PREFIX = "TPC-H ";
	private static final Set<String> WARP_SYNCED_WORLDS = new HashSet<>();
	private static final Set<String> HOME_SYNCED_WORLDS = new HashSet<>();

	private XaeroCompat() {
	}

	public static boolean applySyncPayload(XaeroSyncPayload payload) {
		MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
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
		MinimapWorldRootContainer currentRoot = worldManager.getCurrentRootContainer();
		for (Map.Entry<String, List<XaeroSyncEntry>> entry : byWorld.entrySet()) {
			String worldId = entry.getKey();
			MinimapWorld world = findWorld(worldManager, currentRoot, worldId);
			if (world == null) {
				continue;
			}

			List<Waypoint> waypoints = toWaypoints(entry.getValue(), type);
			persistWaypoints(world, entry.getValue(), waypoints, type, persist, warpSetName, homeSetName);
			markWorldSynced(worldId, type);
		}

		clearMissingWorlds(worldManager, byWorld.keySet(), type, persist, warpSetName, homeSetName);
	}

	private static void clearMissingWorlds(MinimapWorldManager worldManager, Set<String> activeWorlds,
			EntryType type, boolean persist, String warpSetName, String homeSetName) {
		Set<String> trackedWorlds = type == EntryType.WARP ? WARP_SYNCED_WORLDS : HOME_SYNCED_WORLDS;
		Set<String> previousWorlds = new HashSet<>(trackedWorlds);
		previousWorlds.removeAll(activeWorlds);
		for (String worldId : previousWorlds) {
			MinimapWorldRootContainer currentRoot = worldManager.getCurrentRootContainer();
			MinimapWorld world = findWorld(worldManager, currentRoot, worldId);
			if (world == null) {
				trackedWorlds.remove(worldId);
				continue;
			}
			persistWaypoints(world, List.of(), List.of(), type, persist, warpSetName, homeSetName);
			trackedWorlds.remove(worldId);
		}
	}

	private static Map<String, List<XaeroSyncEntry>> groupByWorld(List<XaeroSyncEntry> entries) {
		Map<String, List<XaeroSyncEntry>> result = new HashMap<>();
		for (XaeroSyncEntry entry : entries) {
			result.computeIfAbsent(entry.worldId(), key -> new ArrayList<>()).add(entry);
		}
		return result;
	}

	private static MinimapWorld findWorld(MinimapWorldManager worldManager, MinimapWorldRootContainer currentRoot,
			String worldId) {
		if (currentRoot != null) {
			for (MinimapWorld world : currentRoot.getAllWorldsIterable()) {
				String currentId = WorldResolver.getDimensionId(world.getDimId());
				if (worldId.equals(currentId)) {
					return world;
				}
			}
		}
		for (MinimapWorldRootContainer root : worldManager.getRootContainers()) {
			for (MinimapWorld world : root.getAllWorldsIterable()) {
				String currentId = WorldResolver.getDimensionId(world.getDimId());
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

	private static void markWorldSynced(String worldId, EntryType type) {
		if (type == EntryType.WARP) {
			WARP_SYNCED_WORLDS.add(worldId);
		} else {
			HOME_SYNCED_WORLDS.add(worldId);
		}
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

	private enum EntryType {
		WARP,
		HOME
	}
}
