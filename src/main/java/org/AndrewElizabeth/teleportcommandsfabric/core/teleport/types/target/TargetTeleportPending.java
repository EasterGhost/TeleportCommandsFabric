package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportOperation;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTargetResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TargetTeleportPending implements TeleportOperation {
	private final UUID playerUuid;
	private final long pendingSequence;
	private final TeleportRequest request;
	private final CompletableFuture<TeleportTargetResult> targetFuture = new CompletableFuture<>();
	private final CompletableFuture<TeleportStatus> resultFuture = new CompletableFuture<>();
	private final long createTick;
	private boolean queued;
	private boolean preloadStarted;

	public TargetTeleportPending(UUID playerUuid, long pendingSequence, TeleportRequest request, long createTick) {
		this.playerUuid = playerUuid;
		this.pendingSequence = pendingSequence;
		this.request = request;
		this.createTick = createTick;
	}

	@Override
	public UUID playerUuid() { return playerUuid; }
	@Override
	public long pendingSequence() { return pendingSequence; }
	public TeleportRequest request() { return request; }
	@Override
	public CompletableFuture<TeleportStatus> resultFuture() { return resultFuture; }
	@Override
	public long createTick() { return createTick; }
	@Override
	public int delayTicks() { return request.options().delayTicks(); }
	@Override
	public long cooldownMillis() { return request.options().cooldownMillis(); }
	@Override
	public boolean recordPrevious() { return request.options().recordPrevious(); }

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
}

