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
	static final String TARGET_KEY = "tpc.target";
	private static final String DEATH_GROUP_ID = "journeymap_death";
	private static final int DEATH_MATCH_RADIUS = 1;
	private static final String MARKER_VALUE = "true";

	private JourneyMapWaypointCommandHelper() {
	}

	static void tag(Waypoint waypoint, SyncedWaypointKind kind, String name, String commandTarget) {
		waypoint.setCustomData(MARKER_KEY, MARKER_VALUE);
		waypoint.setCustomData(KIND_KEY, kind.name());
		waypoint.setCustomData(NAME_KEY, name);
		waypoint.setCustomData(TARGET_KEY, commandTarget);
	}

	static boolean isTpcWaypoint(Waypoint waypoint) {
		return waypoint != null && MARKER_VALUE.equals(waypoint.getCustomData(MARKER_KEY));
	}

	static String key(SyncedMapWaypoint waypoint) {
		return waypoint.kind().name() + '\u0000' + waypoint.commandTarget();
	}

	static String key(Waypoint waypoint) {
		WaypointCommandTarget target = target(waypoint);
		return target == null ? null : target.kind().name() + '\u0000' + target.commandTarget();
	}

	public static String buildTeleportCommand(Waypoint waypoint) {
		WaypointCommandTarget target = target(waypoint);
		if (target == null) {
			return matchesCurrentDeathLocation(waypoint) ? command("back") + " death" : null;
		}
		return switch (target.kind()) {
		case HOME -> command("home") + " " + CommandArgumentUtils.quote(target.commandTarget());
		case WARP -> command("warp") + " " + CommandArgumentUtils.quote(target.commandTarget());
		case SHARED_HOME -> "teleportcommandsfabric:sharedhome " + target.commandTarget();
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
		case HOME -> "teleportcommandsfabric:maphome " + CommandArgumentUtils.quote(target.commandTarget()) + " false";
		case WARP -> "teleportcommandsfabric:mapwarp " + CommandArgumentUtils.quote(target.commandTarget()) + " false";
		case SHARED_HOME -> "teleportcommandsfabric:mapsharedhome " + target.commandTarget() + " false";
		};
	}

	private static WaypointCommandTarget target(Waypoint waypoint) {
		if (!isTpcWaypoint(waypoint)) {
			return null;
		}
		String kindValue = waypoint.getCustomData(KIND_KEY);
		String name = waypoint.getCustomData(NAME_KEY);
		String commandTarget = waypoint.getCustomData(TARGET_KEY);
		if (kindValue == null || name == null || name.isBlank()) {
			return null;
		}
		try {
			SyncedWaypointKind kind = SyncedWaypointKind.valueOf(kindValue);
			if (commandTarget == null || commandTarget.isBlank()) {
				if (kind == SyncedWaypointKind.SHARED_HOME) {
					return null;
				}
				commandTarget = name;
			}
			return new WaypointCommandTarget(kind, commandTarget);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private record WaypointCommandTarget(SyncedWaypointKind kind, String commandTarget) {
	}
}
