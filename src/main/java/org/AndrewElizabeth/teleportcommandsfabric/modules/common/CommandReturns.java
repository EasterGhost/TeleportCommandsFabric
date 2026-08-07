package org.AndrewElizabeth.teleportcommandsfabric.modules.common;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;

public final class CommandReturns {
	public static final int FAILED = -1;
	public static final int ACCEPTED_ASYNC = 0;
	public static final int COMPLETED_SYNC = 1;

	private CommandReturns() {
	}

	public static int forTeleportStatus(TeleportStatus status) {
		if (status == TeleportStatus.ACCEPTED) {
			return ACCEPTED_ASYNC;
		}
		return COMPLETED_SYNC;
	}
}
