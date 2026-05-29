package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record TargetTeleportExecution(
		TargetTeleportPending pending,
		TeleportTarget target) {
	public TargetTeleportExecution {
		Objects.requireNonNull(pending, "pending");
		Objects.requireNonNull(target, "target");
	}

	public UUID playerUuid() {
		return pending.playerUuid();
	}

	public long pendingSequence() {
		return pending.pendingSequence();
	}

	public TargetTeleportOptions options() {
		return pending.request().options();
	}

	public CompletableFuture<TeleportStatus> resultFuture() {
		return pending.resultFuture();
	}
}
