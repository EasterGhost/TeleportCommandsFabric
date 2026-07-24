package org.AndrewElizabeth.teleportcommandsfabric.integration.xaero;

import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
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
import java.util.Map;
import java.util.Set;

final class XaeroCompat {
	private static final Set<String> WARP_SYNCED_WORLDS = new HashSet<>();
	private static final Set<String> HOME_SYNCED_WORLDS = new HashSet<>();
	private static final Set<String> SHARED_HOME_SYNCED_WORLDS = new HashSet<>();

	private XaeroCompat() {
	}

	static boolean applySnapshot(MapWaypointSnapshot snapshot) {
		MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
		if (minimapSession == null) {
			return false;
		}

		MinimapWorldManager worldManager = minimapSession.getWorldManager();
		if (worldManager == null) {
			return false;
		}

		boolean persist = snapshot.persistWaypointSets();
		String warpSetName = XaeroWaypointSets.normalizeSetName(snapshot.warpGroupName(), XaeroWaypointType.WARP);
		String homeSetName = XaeroWaypointSets.normalizeSetName(snapshot.homeGroupName(), XaeroWaypointType.HOME);

		boolean warpsApplied = applyEntries(worldManager, entriesOf(snapshot, SyncedWaypointKind.WARP),
				XaeroWaypointType.WARP, persist, warpSetName, homeSetName);
		boolean homesApplied = applyEntries(worldManager, entriesOf(snapshot, SyncedWaypointKind.HOME),
				XaeroWaypointType.HOME,
				persist, warpSetName, homeSetName);
		boolean sharedHomesApplied = applyEntries(worldManager, entriesOf(snapshot, SyncedWaypointKind.SHARED_HOME),
				XaeroWaypointType.SHARED_HOME, false, warpSetName, homeSetName);
		return warpsApplied && homesApplied && sharedHomesApplied;
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
			XaeroWaypointType type, boolean persist, String warpSetName, String homeSetName) {
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

			XaeroWaypointWriter.applyWaypoints(world, entry.getValue(), type, persist, warpSetName, homeSetName);
			markWorldSynced(worldId, type);
		}

		clearMissingWorlds(worldManager, byWorld.keySet(), type, persist, warpSetName, homeSetName);
		return allWorldsApplied;
	}

	private static void clearMissingWorlds(MinimapWorldManager worldManager, Set<String> activeWorlds,
			XaeroWaypointType type, boolean persist, String warpSetName, String homeSetName) {
		Set<String> trackedWorlds = trackedWorlds(type);
		Set<String> previousWorlds = new HashSet<>(trackedWorlds);
		previousWorlds.removeAll(activeWorlds);
		for (String worldId : previousWorlds) {
			MinimapWorldRootContainer currentRoot = worldManager.getCurrentRootContainer();
			MinimapWorld world = findWorld(worldManager, currentRoot, worldId);
			if (world == null) {
				trackedWorlds.remove(worldId);
				continue;
			}
			XaeroWaypointWriter.applyWaypoints(world, List.of(), type, persist, warpSetName, homeSetName);
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

	private static void markWorldSynced(String worldId, XaeroWaypointType type) {
		trackedWorlds(type).add(worldId);
	}

	private static Set<String> trackedWorlds(XaeroWaypointType type) {
		return switch (type) {
		case WARP -> WARP_SYNCED_WORLDS;
		case HOME -> HOME_SYNCED_WORLDS;
		case SHARED_HOME -> SHARED_HOME_SYNCED_WORLDS;
		};
	}
}
