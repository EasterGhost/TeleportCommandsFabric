package org.AndrewElizabeth.teleportcommandsfabric.integration.xaero;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.client.ClientMapWaypointSnapshots;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedMapWaypoint;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.SyncedWaypointKind;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;

import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointPurpose;

public final class XaeroWaypointCommandHelper {
	private XaeroWaypointCommandHelper() {
	}

	public static String buildHideCommand(Waypoint waypoint) {
		if (waypoint == null) {
			return null;
		}

		return buildHideCommand(waypoint.getName(), waypoint.getX(), waypoint.getY(), waypoint.getZ());
	}

	public static String buildHideCommand(xaero.map.mods.gui.Waypoint waypoint) {
		if (waypoint == null) {
			return null;
		}

		return buildHideCommand(waypoint.getName(), waypoint.getX(), waypoint.getY(), waypoint.getZ());
	}

	public static String buildTeleportCommand(Waypoint waypoint) {
		if (waypoint == null) {
			return null;
		}
		if (waypoint.getPurpose() == WaypointPurpose.DEATH) {
			return command("back") + " death";
		}
		return buildTaggedTeleportCommand(waypoint.getName(), waypoint.getX(), waypoint.getY(), waypoint.getZ());
	}

	public static String buildTeleportCommand(xaero.map.mods.gui.Waypoint waypoint) {
		if (waypoint == null) {
			return null;
		}
		return buildTaggedTeleportCommand(waypoint.getName(), waypoint.getX(), waypoint.getY(), waypoint.getZ());
	}

	private static String buildHideCommand(String name, int x, int y, int z) {
		if (name == null) {
			return null;
		}

		if (name.startsWith(XaeroWaypointTags.WARP_PREFIX)) {
			return buildHideCommandLiteral("teleportcommandsfabric:mapwarp",
					name.substring(XaeroWaypointTags.WARP_PREFIX.length()).trim());
		}
		if (name.startsWith(XaeroWaypointTags.HOME_PREFIX)) {
			return buildHideCommandLiteral("teleportcommandsfabric:maphome",
					name.substring(XaeroWaypointTags.HOME_PREFIX.length()).trim());
		}
		if (name.startsWith(XaeroWaypointTags.SHARED_HOME_PREFIX)) {
			return sharedCommand("teleportcommandsfabric:mapsharedhome", name, x, y, z, " false");
		}

		return null;
	}

	private static String buildHideCommandLiteral(String command, String name) {
		return command + " " + CommandArgumentUtils.quote(name) + " false";
	}

	private static String buildTaggedTeleportCommand(String name, int x, int y, int z) {
		if (name == null) {
			return null;
		}

		String normalizedName = XaeroWaypointTags.stripPrefix(name);
		if (normalizedName.isBlank()) {
			return null;
		}

		if (name.startsWith(XaeroWaypointTags.WARP_PREFIX)) {
			return command("warp") + " " + CommandArgumentUtils.quote(normalizedName);
		}
		if (name.startsWith(XaeroWaypointTags.HOME_PREFIX)) {
			return command("home") + " " + CommandArgumentUtils.quote(normalizedName);
		}
		if (name.startsWith(XaeroWaypointTags.SHARED_HOME_PREFIX)) {
			return sharedCommand("teleportcommandsfabric:sharedhome", name, x, y, z, "");
		}
		return null;
	}

	private static String sharedCommand(String root, String taggedName, int x, int y, int z, String suffix) {
		String name = taggedName.substring(XaeroWaypointTags.SHARED_HOME_PREFIX.length()).trim();
		SyncedMapWaypoint match = null;
		for (SyncedMapWaypoint waypoint : ClientMapWaypointSnapshots.latest().waypoints()) {
			if (waypoint.kind() != SyncedWaypointKind.SHARED_HOME || !waypoint.name().equals(name)) {
				continue;
			}
			if (x != Integer.MIN_VALUE
					&& (waypoint.x() != x || waypoint.y() != y || waypoint.z() != z)) {
				continue;
			}
			if (match != null) {
				return null;
			}
			match = waypoint;
		}
		return match == null ? null : root + " " + match.commandTarget() + suffix;
	}

	private static String command(String root) {
		return ClientMapWaypointSnapshots.latestFromLegacyXaero() ? root : "teleportcommandsfabric:" + root;
	}

}
