package org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.client;

import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.MinimapWorldManager;
import xaero.hud.minimap.world.container.MinimapWorldRootContainer;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedMapWaypoint;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedWaypointKind;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class XaeroCompat {
	private static final String DEFAULT_SET_SENTINEL = "default";
	private static final String CURRENT_SET_SENTINEL = "current";
	private static final String LEGACY_WARP_SET_SENTINEL = "teleportcommands warps";
	private static final String LEGACY_HOME_SET_SENTINEL = "teleportcommands homes";
	private static final Set<String> WARP_SYNCED_WORLDS = new HashSet<>();
	private static final Set<String> HOME_SYNCED_WORLDS = new HashSet<>();

	private XaeroCompat() {
	}

	public static boolean applySnapshot(MapWaypointSnapshot snapshot) {
		MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
		if (minimapSession == null) {
			return false;
		}

		MinimapWorldManager worldManager = minimapSession.getWorldManager();
		if (worldManager == null) {
			return false;
		}

		boolean persist = snapshot.persistWaypointSets();
		String warpSetName = normalizeSetName(snapshot.warpGroupName(), EntryType.WARP);
		String homeSetName = normalizeSetName(snapshot.homeGroupName(), EntryType.HOME);

		boolean warpsApplied = applyEntries(worldManager, entriesOf(snapshot, SyncedWaypointKind.WARP), EntryType.WARP,
				persist, warpSetName, homeSetName);
		boolean homesApplied = applyEntries(worldManager, entriesOf(snapshot, SyncedWaypointKind.HOME), EntryType.HOME,
				persist, warpSetName, homeSetName);
		return warpsApplied && homesApplied;
	}

	private static List<SyncedMapWaypoint> entriesOf(MapWaypointSnapshot snapshot, SyncedWaypointKind kind) {
		List<SyncedMapWaypoint> result = new ArrayList<>();
		for (SyncedMapWaypoint waypoint : snapshot.waypoints()) {
			if (waypoint.kind() == kind) {
				result.add(waypoint);
			}
		}
		return result;
	}

	private static boolean applyEntries(MinimapWorldManager worldManager, List<SyncedMapWaypoint> entries,
			EntryType type, boolean persist, String warpSetName, String homeSetName) {
		Map<String, List<SyncedMapWaypoint>> byWorld = groupByWorld(entries);
		MinimapWorldRootContainer currentRoot = worldManager.getCurrentRootContainer();
		boolean allWorldsApplied = true;
		for (Map.Entry<String, List<SyncedMapWaypoint>> entry : byWorld.entrySet()) {
			String worldId = entry.getKey();
			MinimapWorld world = findWorld(worldManager, currentRoot, worldId);
			if (world == null) {
				allWorldsApplied = false;
				continue;
			}

			applyWaypoints(world, entry.getValue(), type, persist, warpSetName, homeSetName);
			markWorldSynced(worldId, type);
		}

		clearMissingWorlds(worldManager, byWorld.keySet(), type, persist, warpSetName, homeSetName);
		return allWorldsApplied;
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
			applyWaypoints(world, List.of(), type, persist, warpSetName, homeSetName);
			trackedWorlds.remove(worldId);
		}
	}

	private static Map<String, List<SyncedMapWaypoint>> groupByWorld(List<SyncedMapWaypoint> entries) {
		Map<String, List<SyncedMapWaypoint>> result = new HashMap<>();
		for (SyncedMapWaypoint entry : entries) {
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

	private static List<Waypoint> toTaggedWaypoints(List<SyncedMapWaypoint> entries, EntryType type,
			boolean temporary) {
		List<Waypoint> waypoints = new ArrayList<>(entries.size());
		WaypointColor color = type == EntryType.WARP ? WaypointColor.BLUE : WaypointColor.GREEN;
		String symbol = type == EntryType.WARP ? "W" : "H";
		String prefix = XaeroWaypointTags.prefix(type == EntryType.WARP);

		for (SyncedMapWaypoint entry : entries) {
			Waypoint waypoint = new Waypoint(entry.x(), entry.y(), entry.z(),
					prefix + entry.name(), symbol, color, WaypointPurpose.NORMAL);
			waypoint.setYIncluded(true);
			waypoint.setTemporary(temporary);
			waypoints.add(waypoint);
		}
		return waypoints;
	}

	private static void applyWaypoints(MinimapWorld world, List<SyncedMapWaypoint> entries, EntryType type,
			boolean persist, String warpSetName, String homeSetName) {
		String setName = type == EntryType.WARP ? warpSetName : homeSetName;
		boolean useDefaultSet = isDefaultSet(setName);
		WaypointSet currentSet = world.getCurrentWaypointSet();
		if (!persist) {
			if (!useDefaultSet) {
				removeTaggedWaypoints(world.getWaypointSet(setName), type);
			}
			replaceTaggedWaypoints(currentSet, entries, type, true);
			return;
		}

		WaypointSet set = useDefaultSet ? currentSet : world.getWaypointSet(setName);
		if (!useDefaultSet && currentSet != null) {
			removeTaggedWaypoints(currentSet, type);
		}
		if (set == null && !useDefaultSet) {
			world.addWaypointSet(setName);
			set = world.getWaypointSet(setName);
		}
		replaceTaggedWaypoints(set, entries, type, false);
	}

	private static void markWorldSynced(String worldId, EntryType type) {
		if (type == EntryType.WARP) {
			WARP_SYNCED_WORLDS.add(worldId);
		} else {
			HOME_SYNCED_WORLDS.add(worldId);
		}
	}

	private static void removeTaggedWaypoints(WaypointSet set, EntryType type) {
		if (set == null) {
			return;
		}

		String prefix = XaeroWaypointTags.prefix(type == EntryType.WARP);
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

	private static void replaceTaggedWaypoints(WaypointSet set, List<SyncedMapWaypoint> entries, EntryType type,
			boolean temporary) {
		if (set == null) {
			return;
		}

		removeTaggedWaypoints(set, type);
		set.addAll(toTaggedWaypoints(entries, type, temporary));
	}

	private static boolean isDefaultSet(String setName) {
		if (setName == null || setName.isBlank()) {
			return true;
		}
		String normalized = setName.trim().toLowerCase(Locale.ROOT);
		return DEFAULT_SET_SENTINEL.equals(normalized)
				|| CURRENT_SET_SENTINEL.equals(normalized);
	}

	private static String normalizeSetName(String setName, EntryType type) {
		if (setName == null) {
			return null;
		}

		String trimmed = setName.trim();
		String normalized = trimmed.toLowerCase(Locale.ROOT);
		if (DEFAULT_SET_SENTINEL.equals(normalized) || CURRENT_SET_SENTINEL.equals(normalized)) {
			return DEFAULT_SET_SENTINEL;
		}

		if (type == EntryType.WARP && LEGACY_WARP_SET_SENTINEL.equals(normalized)) {
			return DEFAULT_SET_SENTINEL;
		}

		if (type == EntryType.HOME && LEGACY_HOME_SET_SENTINEL.equals(normalized)) {
			return DEFAULT_SET_SENTINEL;
		}

		return trimmed;
	}

	private enum EntryType {
		WARP, HOME
	}
}
