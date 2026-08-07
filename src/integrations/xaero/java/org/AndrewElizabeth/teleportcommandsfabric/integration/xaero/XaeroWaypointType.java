package org.AndrewElizabeth.teleportcommandsfabric.integration.xaero;

import xaero.hud.minimap.waypoint.WaypointColor;

enum XaeroWaypointType {
	WARP(WaypointColor.BLUE, "W"),
	HOME(WaypointColor.GREEN, "H"),
	SHARED_HOME(WaypointColor.YELLOW, "S");

	private final WaypointColor color;
	private final String symbol;

	XaeroWaypointType(WaypointColor color, String symbol) {
		this.color = color;
		this.symbol = symbol;
	}

	WaypointColor color() {
		return color;
	}

	String symbol() {
		return symbol;
	}

	String prefix() {
		return XaeroWaypointTags.prefix(this);
	}
}
