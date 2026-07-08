package org.AndrewElizabeth.teleportcommandsfabric.integration.xaero;

import java.util.Locale;

final class XaeroWaypointSets {
	private static final String DEFAULT_SET_SENTINEL = "default";
	private static final String CURRENT_SET_SENTINEL = "current";
	private static final String LEGACY_WARP_SET_SENTINEL = "teleportcommands warps";
	private static final String LEGACY_HOME_SET_SENTINEL = "teleportcommands homes";

	private XaeroWaypointSets() {
	}

	static boolean isDefaultSet(String setName) {
		if (setName == null || setName.isBlank()) {
			return true;
		}
		String normalized = setName.trim().toLowerCase(Locale.ROOT);
		return DEFAULT_SET_SENTINEL.equals(normalized)
				|| CURRENT_SET_SENTINEL.equals(normalized);
	}

	static String normalizeSetName(String setName, XaeroWaypointType type) {
		if (setName == null) {
			return null;
		}

		String trimmed = setName.trim();
		String normalized = trimmed.toLowerCase(Locale.ROOT);
		if (DEFAULT_SET_SENTINEL.equals(normalized) || CURRENT_SET_SENTINEL.equals(normalized)) {
			return DEFAULT_SET_SENTINEL;
		}
		if (type == XaeroWaypointType.WARP && LEGACY_WARP_SET_SENTINEL.equals(normalized)) {
			return DEFAULT_SET_SENTINEL;
		}
		if (type == XaeroWaypointType.HOME && LEGACY_HOME_SET_SENTINEL.equals(normalized)) {
			return DEFAULT_SET_SENTINEL;
		}
		return trimmed;
	}
}
