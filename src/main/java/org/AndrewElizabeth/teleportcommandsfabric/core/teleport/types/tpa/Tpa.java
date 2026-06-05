package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa;

import java.util.UUID;

public final class Tpa {
	private Tpa() {
	}

	public enum Type {
		TPA, TPAHERE
	}

	public record Session(
			UUID sessionId,
			UUID sender,
			UUID target,
			Type type,
			long expiredTime,
			int delayTicks,
			long cooldownMillis,
			boolean recordPrevious) {

		public boolean isExpired(long now) {
			return now >= expiredTime;
		}
	}
}