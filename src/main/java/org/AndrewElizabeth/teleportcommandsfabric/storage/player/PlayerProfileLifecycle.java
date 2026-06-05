package org.AndrewElizabeth.teleportcommandsfabric.storage.player;

import org.AndrewElizabeth.teleportcommandsfabric.storage.ProfileLifecycleSupport;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import java.io.IOException;
import java.util.UUID;

public final class PlayerProfileLifecycle {

	private PlayerProfileLifecycle() {
	}

	public static LoadResult loadOrCreate(PlayerProfileIO io, UUID uuid, boolean deleteInvalidHomes) throws IOException {
		PlayerProfile profile = io.load(uuid).orElseGet(() -> new PlayerProfile(uuid));
		boolean changed = prepareLoaded(profile, deleteInvalidHomes);
		return new LoadResult(profile, changed);
	}

	public static boolean prepareLoaded(PlayerProfile profile, boolean deleteInvalidHomes) {
		return ProfileLifecycleSupport.prepareLoaded(
				deleteInvalidHomes,
				profile::refreshHomeState,
				() -> profile.removeInvalidHomes(PlayerProfileLifecycle::isInvalidLocation),
				profile::rebuildHomeNameIndex);
	}

	public static boolean flush(PlayerProfileIO io, UUID uuid, PlayerProfile profile) throws IOException {
		return ProfileLifecycleSupport.flush(profile, PlayerProfile::isEmpty, () -> io.delete(uuid), io::save);
	}

	public record LoadResult(PlayerProfile profile, boolean changed) {
	}

	private static boolean isInvalidLocation(NamedLocation location) {
		return location == null || WorldResolver.getLevel(location.getDimension()).isEmpty();
	}
}
