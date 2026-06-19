package org.AndrewElizabeth.teleportcommandsfabric.storage.player;

import org.AndrewElizabeth.teleportcommandsfabric.storage.ProfileLifecycleSupport;

import java.io.IOException;
import java.util.UUID;

public final class PlayerProfileLifecycle {

	private PlayerProfileLifecycle() {
	}

	public static LoadResult loadOrCreate(PlayerProfileIO io, UUID uuid) throws IOException {
		PlayerProfile profile = io.load(uuid).orElseGet(() -> new PlayerProfile(uuid));
		boolean changed = prepareLoaded(profile);
		return new LoadResult(profile, changed);
	}

	public static boolean prepareLoaded(PlayerProfile profile) {
		boolean changed = profile.refreshHomeState();
		profile.rebuildHomeNameIndex();
		return changed;
	}

	public static boolean flush(PlayerProfileIO io, UUID uuid, PlayerProfile profile) throws IOException {
		return ProfileLifecycleSupport.flush(profile, PlayerProfile::isEmpty, () -> io.delete(uuid), io::save);
	}

	public record LoadResult(PlayerProfile profile, boolean changed) {
	}
}
