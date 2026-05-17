package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public record TeleportRequest(
		CompletableFuture<TeleportTargetResult> targetFuture,
		TeleportOptions options) {
	public TeleportRequest {
		Objects.requireNonNull(targetFuture, "targetFuture");
		options = options == null ? TeleportOptions.DEFAULT : options;
	}

	public static TeleportRequest of(CompletableFuture<TeleportTargetResult> targetFuture, TeleportOptions options) {
		return new TeleportRequest(targetFuture, options);
	}

	public static TeleportRequest resolved(TeleportTarget target, TeleportOptions options) {
		return new TeleportRequest(CompletableFuture.completedFuture(TeleportTargetResult.resolved(target)), options);
	}
}
