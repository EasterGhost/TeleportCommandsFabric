package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.rtp;

public record RtpRequest(int minRadius, int maxRadius, int maxAttempts, int delayTicks, long cooldownMillis,
		boolean recordPrevious) {
	public RtpRequest {
		if (minRadius < 0) {
			throw new IllegalArgumentException("minRadius must be non-negative");
		}
		if (maxRadius < minRadius) {
			throw new IllegalArgumentException("maxRadius must be greater than or equal to minRadius");
		}
		if (maxAttempts < 0) {
			throw new IllegalArgumentException("maxAttempts must be non-negative");
		}
		if (delayTicks < 0) {
			throw new IllegalArgumentException("delayTicks must be non-negative");
		}
		if (cooldownMillis < 0L) {
			throw new IllegalArgumentException("cooldownMillis must be non-negative");
		}
	}
}
