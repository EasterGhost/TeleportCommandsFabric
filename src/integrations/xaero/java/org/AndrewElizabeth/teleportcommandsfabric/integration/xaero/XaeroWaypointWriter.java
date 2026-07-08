package org.AndrewElizabeth.teleportcommandsfabric.integration.xaero;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedMapWaypoint;

import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;

import java.util.ArrayList;
import java.util.List;

final class XaeroWaypointWriter {
	private XaeroWaypointWriter() {
	}

	static void applyWaypoints(MinimapWorld world, List<SyncedMapWaypoint> entries, XaeroWaypointType type,
			boolean persist, String warpSetName, String homeSetName) {
		String setName = type == XaeroWaypointType.WARP ? warpSetName : homeSetName;
		boolean useDefaultSet = XaeroWaypointSets.isDefaultSet(setName);
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

	static List<Waypoint> toTaggedWaypoints(List<SyncedMapWaypoint> entries, XaeroWaypointType type,
			boolean temporary) {
		List<Waypoint> waypoints = new ArrayList<>(entries.size());
		String prefix = type.prefix();
		for (SyncedMapWaypoint entry : entries) {
			Waypoint waypoint = new Waypoint(entry.x(), entry.y(), entry.z(),
					prefix + entry.name(), type.symbol(), type.color(), WaypointPurpose.NORMAL);
			waypoint.setYIncluded(true);
			waypoint.setTemporary(temporary);
			waypoints.add(waypoint);
		}
		return waypoints;
	}

	private static void removeTaggedWaypoints(WaypointSet set, XaeroWaypointType type) {
		if (set == null) {
			return;
		}

		String prefix = type.prefix();
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

	private static void replaceTaggedWaypoints(WaypointSet set, List<SyncedMapWaypoint> entries,
			XaeroWaypointType type, boolean temporary) {
		if (set == null) {
			return;
		}

		removeTaggedWaypoints(set, type);
		set.addAll(toTaggedWaypoints(entries, type, temporary));
	}
}
