package org.AndrewElizabeth.teleportcommandsfabric.utils;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;

public final class DebugLog {
	private static volatile boolean enabled;

	private DebugLog() {
	}

	public static void setEnabled(boolean enabled) {
		DebugLog.enabled = enabled;
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void debug(String message, Object... args) {
		if (enabled) {
			ModConstants.LOGGER.debug(message, args);
		}
	}

	public static void info(String message, Object... args) {
		if (enabled) {
			ModConstants.LOGGER.info(message, args);
		}
	}

	public static void warn(String message, Object... args) {
		if (enabled) {
			ModConstants.LOGGER.warn(message, args);
		}
	}

	public static void error(String message, Object... args) {
		if (enabled) {
			ModConstants.LOGGER.error(message, args);
		}
	}
}
