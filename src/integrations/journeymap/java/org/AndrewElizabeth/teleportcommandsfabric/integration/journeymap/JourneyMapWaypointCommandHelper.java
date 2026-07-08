package org.AndrewElizabeth.teleportcommandsfabric.integration.journeymap;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.client.ClientMapWaypointSnapshots;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedWaypointKind;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedDeathLocation;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedMapWaypoint;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;

import journeymap.api.v2.common.waypoint.Waypoint;

import net.minecraft.core.BlockPos;

public final class JourneyMapWaypointCommandHelper {
	static final String MARKER_KEY = "tpc.marker";
	static final String KIND_KEY = "tpc.kind";
	static final String NAME_KEY = "tpc.name";
	private static final String DEATH_GROUP_ID = "journeymap_death";
	private static final int DEATH_MATCH_RADIUS = 1;
	private static final String MARKER_VALUE = "true";

	private JourneyMapWaypointCommandHelper() {
	}

	static void tag(Waypoint waypoint, SyncedWaypointKind kind, String name) {
		waypoint.setCustomData(MARKER_KEY, MARKER_VALUE);
		waypoint.setCustomData(KIND_KEY, kind.name());
		waypoint.setCustomData(NAME_KEY, name);
	}

	static boolean isTpcWaypoint(Waypoint waypoint) {
		return waypoint != null && MARKER_VALUE.equals(waypoint.getCustomData(MARKER_KEY));
	}

	static String key(SyncedMapWaypoint waypoint) {
		return waypoint.kind().name() + '\u0000' + waypoint.name();
	}

	static String key(Waypoint waypoint) {
		WaypointCommandTarget target = target(waypoint);
		return target == null ? null : target.kind().name() + '\u0000' + target.name();
	}

	public static String buildTeleportCommand(Waypoint waypoint) {
		WaypointCommandTarget target = target(waypoint);
		if (target == null) {
			return matchesCurrentDeathLocation(waypoint) ? command("back") + " death" : null;
		}
		return switch (target.kind()) {
		case HOME -> command("home") + " " + CommandArgumentUtils.quote(target.name());
		case WARP -> command("warp") + " " + CommandArgumentUtils.quote(target.name());
		};
	}

	private static String command(String root) {
		return ClientMapWaypointSnapshots.latestFromLegacyXaero() ? root : "teleportcommandsfabric:" + root;
	}

	private static boolean isDeathWaypoint(Waypoint waypoint) {
		return waypoint != null && DEATH_GROUP_ID.equals(waypoint.getGroupId());
	}

	private static boolean matchesCurrentDeathLocation(Waypoint waypoint) {
		if (!isDeathWaypoint(waypoint)) {
			return false;
		}
		SyncedDeathLocation location = ClientMapWaypointSnapshots.latest().deathLocation();
		return location.hasLocation()
				&& location.worldId().equals(waypoint.getPrimaryDimension())
				&& blockPosMatches(location, waypoint.getBlockPos());
	}

	private static boolean blockPosMatches(SyncedDeathLocation location, BlockPos pos) {
		return pos != null
				&& Math.abs(location.x() - pos.getX()) <= DEATH_MATCH_RADIUS
				&& Math.abs(location.y() - pos.getY()) <= DEATH_MATCH_RADIUS
				&& Math.abs(location.z() - pos.getZ()) <= DEATH_MATCH_RADIUS;
	}

	static String buildHideCommand(Waypoint waypoint) {
		WaypointCommandTarget target = target(waypoint);
		if (target == null) {
			return null;
		}
		return switch (target.kind()) {
		case HOME -> "teleportcommandsfabric:maphome " + CommandArgumentUtils.quote(target.name()) + " false";
		case WARP -> "teleportcommandsfabric:mapwarp " + CommandArgumentUtils.quote(target.name()) + " false";
		};
	}

	private static WaypointCommandTarget target(Waypoint waypoint) {
		if (!isTpcWaypoint(waypoint)) {
			return null;
		}
		String kindValue = waypoint.getCustomData(KIND_KEY);
		String name = waypoint.getCustomData(NAME_KEY);
		if (kindValue == null || name == null || name.isBlank()) {
			return null;
		}
		try {
			return new WaypointCommandTarget(SyncedWaypointKind.valueOf(kindValue), name);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private record WaypointCommandTarget(SyncedWaypointKind kind, String name) {
	}
}
