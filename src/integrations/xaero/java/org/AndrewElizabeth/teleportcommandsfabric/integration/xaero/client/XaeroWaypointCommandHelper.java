package org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.client;

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

		return buildHideCommand(waypoint.getName());
	}

	public static String buildHideCommand(xaero.map.mods.gui.Waypoint waypoint) {
		if (waypoint == null) {
			return null;
		}

		return buildHideCommand(waypoint.getName());
	}

	public static String buildTeleportCommand(Waypoint waypoint) {
		if (waypoint == null) {
			return null;
		}
		if (waypoint.getPurpose() == WaypointPurpose.DEATH) {
			return "back death";
		}
		return buildTaggedTeleportCommand(waypoint.getName());
	}

	public static String buildTeleportCommand(xaero.map.mods.gui.Waypoint waypoint) {
		if (waypoint == null) {
			return null;
		}
		return buildTaggedTeleportCommand(waypoint.getName());
	}

	private static String buildHideCommand(String name) {
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

		return null;
	}

	private static String buildHideCommandLiteral(String command, String name) {
		return command + " " + CommandArgumentUtils.quote(name) + " false";
	}

	private static String buildTaggedTeleportCommand(String name) {
		if (name == null) {
			return null;
		}

		String normalizedName = XaeroWaypointTags.stripPrefix(name);
		if (normalizedName.isBlank()) {
			return null;
		}

		if (name.startsWith(XaeroWaypointTags.WARP_PREFIX)) {
			return "warp " + CommandArgumentUtils.quote(normalizedName);
		}
		if (name.startsWith(XaeroWaypointTags.HOME_PREFIX)) {
			return "home " + CommandArgumentUtils.quote(normalizedName);
		}
		return null;
	}

}
