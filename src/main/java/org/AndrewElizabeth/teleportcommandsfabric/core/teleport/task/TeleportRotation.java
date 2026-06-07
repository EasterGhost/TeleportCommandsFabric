package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task;

public final class TeleportRotation {
	private static volatile boolean restoreEnabled = true;

	private TeleportRotation() {
	}

	public static void setRestoreEnabled(boolean restoreEnabled) {
		TeleportRotation.restoreEnabled = restoreEnabled;
	}

	static boolean isRestoreEnabled() {
		return restoreEnabled;
	}
}
