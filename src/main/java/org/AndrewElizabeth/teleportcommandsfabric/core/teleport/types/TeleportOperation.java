package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface TeleportOperation {
	UUID playerUuid();

	long pendingSequence();

	long createTick();

	int delayTicks();

	long cooldownMillis();

	boolean recordPrevious();

	CompletableFuture<TeleportStatus> resultFuture();

	default long delayUntilTick() {
		return createTick() + delayTicks();
	}

	default boolean isDelayDone(long currentTick) {
		return currentTick >= delayUntilTick();
	}
}
