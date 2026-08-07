package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types;

public sealed interface TeleportTargetResult {
	record Resolved(TeleportTarget target) implements TeleportTargetResult {}
	record Failed(TeleportStatus reason) implements TeleportTargetResult {}

	static TeleportTargetResult resolved(TeleportTarget target) {
		return new Resolved(target);
	}

	static TeleportTargetResult failed(TeleportStatus reason) {
		return new Failed(reason);
	}
}
