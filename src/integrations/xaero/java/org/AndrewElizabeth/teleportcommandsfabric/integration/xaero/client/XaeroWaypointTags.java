package org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.client;

final class XaeroWaypointTags {
	static final String WARP_PREFIX = "TPC-W ";
	static final String HOME_PREFIX = "TPC-H ";

	private XaeroWaypointTags() {
	}

	static String prefix(boolean warp) {
		return warp ? WARP_PREFIX : HOME_PREFIX;
	}

	static String stripPrefix(String name) {
		if (name.startsWith(WARP_PREFIX)) {
			return name.substring(WARP_PREFIX.length()).trim();
		}
		if (name.startsWith(HOME_PREFIX)) {
			return name.substring(HOME_PREFIX.length()).trim();
		}
		return name.trim();
	}
}
