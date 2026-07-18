package org.AndrewElizabeth.teleportcommandsfabric.storage.global;

public final class GlobalProfileTestAccess {
	private GlobalProfileTestAccess() {
	}

	public static boolean prepareLoaded(GlobalProfile profile) {
		return GlobalProfileLifecycle.prepareLoaded(profile);
	}
}
