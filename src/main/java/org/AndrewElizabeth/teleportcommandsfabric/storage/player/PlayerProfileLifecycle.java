package org.AndrewElizabeth.teleportcommandsfabric.storage.player;

import org.AndrewElizabeth.teleportcommandsfabric.storage.ProfileLifecycleSupport;

import java.io.IOException;
import java.util.UUID;

final class PlayerProfileLifecycle {

	private PlayerProfileLifecycle() {
	}

	static LoadResult loadOrCreate(PlayerProfileIO io, UUID uuid) throws IOException {
		PlayerProfile profile = io.load(uuid).orElseGet(() -> new PlayerProfile(uuid));
		boolean changed = prepareLoaded(profile);
		return new LoadResult(profile, changed);
	}

	static boolean prepareLoaded(PlayerProfile profile) {
		boolean changed = profile.refreshHomeState();
		profile.rebuildHomeNameIndex();
		return changed;
	}

	static boolean flush(PlayerProfileIO io, UUID uuid, PlayerProfile profile) throws IOException {
		return ProfileLifecycleSupport.flush(profile, PlayerProfile::isEmpty, () -> io.delete(uuid), io::save);
	}

	record LoadResult(PlayerProfile profile, boolean changed) {
	}
}
