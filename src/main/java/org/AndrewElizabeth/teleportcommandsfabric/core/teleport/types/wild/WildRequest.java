package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.wild;

public record WildRequest(int minRadius, int maxRadius, int delayTicks, long cooldownMillis,
		boolean recordPrevious) {
	public static final int MIN_RADIUS = 128;
	public static final int MAX_RADIUS = 30_000_000;

	public WildRequest {
		if (minRadius < MIN_RADIUS) {
			throw new IllegalArgumentException("minRadius must be at least " + MIN_RADIUS);
		}
		if (maxRadius < minRadius || maxRadius > MAX_RADIUS) {
			throw new IllegalArgumentException("maxRadius must be between minRadius and " + MAX_RADIUS);
		}
		if (delayTicks < 0) {
			throw new IllegalArgumentException("delayTicks must be non-negative");
		}
		if (cooldownMillis < 0L) {
			throw new IllegalArgumentException("cooldownMillis must be non-negative");
		}
	}
}
