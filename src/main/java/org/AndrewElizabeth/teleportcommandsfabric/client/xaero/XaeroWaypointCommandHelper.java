package org.AndrewElizabeth.teleportcommandsfabric.client.xaero;

import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;

import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointPurpose;

import java.util.Locale;

public final class XaeroWaypointCommandHelper {
	private XaeroWaypointCommandHelper() {
	}

	public static String buildHideCommand(Waypoint waypoint, String setName) {
		if (waypoint == null) {
			return null;
		}

		String name = waypoint.getName();
		String symbol = waypoint.getInitials();
		return buildHideCommand(name, symbol, setName);
	}

	public static String buildHideCommand(xaero.map.mods.gui.Waypoint waypoint) {
		if (waypoint == null) {
			return null;
		}

		String name = waypoint.getName();
		String symbol = waypoint.getSymbol();
		String setName = waypoint.getSetName();
		return buildHideCommand(name, symbol, setName);
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

	private static String buildHideCommand(String name, String symbol, String setName) {
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

		String normalizedSetName = normalizeSetName(setName);
		String normalizedWarpSetName = normalizeSetName(XaeroCompat.getCurrentWarpSetName());
		String normalizedHomeSetName = normalizeSetName(XaeroCompat.getCurrentHomeSetName());
		String normalizedSymbol = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
		String trimmedName = name.trim();
		if (trimmedName.isBlank()) {
			return null;
		}

		if (!normalizedSetName.isEmpty() && normalizedSetName.equals(normalizedWarpSetName)
				&& "W".equals(normalizedSymbol)) {
			return buildHideCommandLiteral("teleportcommandsfabric:mapwarp", trimmedName);
		}
		if (!normalizedSetName.isEmpty() && normalizedSetName.equals(normalizedHomeSetName)
				&& "H".equals(normalizedSymbol)) {
			return buildHideCommandLiteral("teleportcommandsfabric:maphome", trimmedName);
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

	private static String normalizeSetName(String setName) {
		return setName == null ? "" : setName.trim().toLowerCase(Locale.ROOT);
	}

}
