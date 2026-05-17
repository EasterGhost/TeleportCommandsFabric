package org.AndrewElizabeth.teleportcommandsfabric.storage.global;

import org.AndrewElizabeth.teleportcommandsfabric.storage.ProfileLifecycleSupport;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import java.io.IOException;

public final class GlobalProfileLifecycle {

	private GlobalProfileLifecycle() {
	}

	public static LoadResult loadOrCreate(GlobalProfileIO io, boolean deleteInvalidWarps) throws IOException {
		GlobalProfile profile = io.load().orElseGet(GlobalProfile::new);
		boolean changed = prepareLoaded(profile, deleteInvalidWarps);
		return new LoadResult(profile, changed);
	}

	public static boolean prepareLoaded(GlobalProfile profile, boolean deleteInvalidWarps) {
		return ProfileLifecycleSupport.prepareLoaded(
				deleteInvalidWarps,
				profile::refreshWarpState,
				() -> profile.removeInvalidWarps(GlobalProfileLifecycle::isInvalidLocation),
				profile::rebuildWarpNameIndex);
	}

	public static boolean flush(GlobalProfileIO io, GlobalProfile profile) throws IOException {
		return ProfileLifecycleSupport.flush(profile, GlobalProfile::isEmpty, io::delete, io::save);
	}

	public record LoadResult(GlobalProfile profile, boolean changed) {
	}

	private static boolean isInvalidLocation(NamedLocation location) {
		return location == null || WorldResolver.getLevel(location.getDimension()).isEmpty();
	}
}
