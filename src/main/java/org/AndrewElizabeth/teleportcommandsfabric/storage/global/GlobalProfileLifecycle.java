package org.AndrewElizabeth.teleportcommandsfabric.storage.global;

import org.AndrewElizabeth.teleportcommandsfabric.storage.ProfileLifecycleSupport;

import java.io.IOException;

public final class GlobalProfileLifecycle {

	private GlobalProfileLifecycle() {
	}

	public static LoadResult loadOrCreate(GlobalProfileIO io) throws IOException {
		GlobalProfile profile = io.load().orElseGet(GlobalProfile::new);
		boolean changed = prepareLoaded(profile);
		return new LoadResult(profile, changed);
	}

	public static boolean prepareLoaded(GlobalProfile profile) {
		boolean changed = profile.refreshWarpState();
		profile.rebuildWarpNameIndex();
		return changed;
	}

	public static boolean flush(GlobalProfileIO io, GlobalProfile profile) throws IOException {
		return ProfileLifecycleSupport.flush(profile, GlobalProfile::isEmpty, io::delete, io::save);
	}

	public record LoadResult(GlobalProfile profile, boolean changed) {
	}
}
