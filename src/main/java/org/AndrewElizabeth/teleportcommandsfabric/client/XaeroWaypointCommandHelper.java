package org.AndrewElizabeth.teleportcommandsfabric.client;

import java.util.Locale;

import xaero.common.minimap.waypoints.Waypoint;

public final class XaeroWaypointCommandHelper {
	private static final String WARP_TAG_PREFIX = "TPC-W ";
	private static final String HOME_TAG_PREFIX = "TPC-H ";

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

	private static String buildHideCommand(String name, String symbol, String setName) {
		if (name == null) {
			return null;
		}

		if (name.startsWith(WARP_TAG_PREFIX)) {
			return "teleportcommandsfabric:mapwarp "
					+ quoteCommandArgument(name.substring(WARP_TAG_PREFIX.length()).trim()) + " false";
		}
		if (name.startsWith(HOME_TAG_PREFIX)) {
			return "teleportcommandsfabric:maphome "
					+ quoteCommandArgument(name.substring(HOME_TAG_PREFIX.length()).trim()) + " false";
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
			return "teleportcommandsfabric:mapwarp " + quoteCommandArgument(trimmedName) + " false";
		}
		if (!normalizedSetName.isEmpty() && normalizedSetName.equals(normalizedHomeSetName)
				&& "H".equals(normalizedSymbol)) {
			return "teleportcommandsfabric:maphome " + quoteCommandArgument(trimmedName) + " false";
		}

		return null;
	}

	private static String normalizeSetName(String setName) {
		return setName == null ? "" : setName.trim().toLowerCase(Locale.ROOT);
	}

	private static String quoteCommandArgument(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}
}
