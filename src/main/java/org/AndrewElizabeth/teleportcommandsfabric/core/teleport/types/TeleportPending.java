package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TeleportPending {
	private final UUID playerUuid;
	private final long pendingSequence;
	private final TeleportRequest request;
	private final CompletableFuture<TeleportTargetResult> targetFuture = new CompletableFuture<>();
	private final CompletableFuture<TeleportStatus> resultFuture = new CompletableFuture<>();
	private final long createTick;
	private boolean queued;
	private boolean preloadStarted;

	public TeleportPending(UUID playerUuid, long pendingSequence, TeleportRequest request, long createTick) {
		this.playerUuid = playerUuid;
		this.pendingSequence = pendingSequence;
		this.request = request;
		this.createTick = createTick;
	}

	public UUID playerUuid() { return playerUuid; }
	public long pendingSequence() { return pendingSequence; }
	public TeleportRequest request() { return request; }
	public CompletableFuture<TeleportStatus> resultFuture() { return resultFuture; }
	public long createTick() { return createTick; }

	public boolean isQueued() { return queued; }
	public void markQueued() { this.queued = true; }
	
	public boolean isPreloadStarted() { return preloadStarted; }
	public void markPreloadStarted() { this.preloadStarted = true; }

	public void completeTarget(TeleportTargetResult result) {
		targetFuture.complete(result);
	}

	public boolean isTargetDone() {
		return targetFuture.isDone();
	}

	public TeleportTargetResult targetResult() {
		return targetFuture.join();
	}

	public long delayUntilTick() {
		return createTick + request.options().delayTicks();
	}

	public boolean isDelayDone(long currentTick) {
		return currentTick >= delayUntilTick();
	}
}
