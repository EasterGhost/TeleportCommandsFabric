package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public record TpaRequest(
		UUID senderUuid,
		UUID targetUuid,
		Tpa.Type type,
		Duration expiry,
		int delayTicks,
		long cooldownMillis,
		boolean recordPrevious) {
	public TpaRequest {
		Objects.requireNonNull(senderUuid, "senderUuid");
		Objects.requireNonNull(targetUuid, "targetUuid");
		Objects.requireNonNull(type, "type");
		expiry = expiry == null ? Duration.ZERO : expiry;
		if (expiry.isNegative()) {
			expiry = Duration.ZERO;
		}
		delayTicks = Math.max(0, delayTicks);
		cooldownMillis = Math.max(0L, cooldownMillis);
	}

	public static TpaRequest of(UUID senderUuid, UUID targetUuid, Tpa.Type type, Duration expiry) {
		return new TpaRequest(senderUuid, targetUuid, type, expiry, 0, 0L, true);
	}
}