package org.AndrewElizabeth.teleportcommandsfabric.utils;

public final class TimeUtils {
	public static final int TICKS_PER_SECOND = 20;
	public static final long MILLIS_PER_SECOND = 1000L;

	private TimeUtils() {
	}

	public static int secondsToTicks(int seconds) {
		return seconds * TICKS_PER_SECOND;
	}

	public static long secondsToMillis(int seconds) {
		return seconds * MILLIS_PER_SECOND;
	}
}
