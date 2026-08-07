package org.AndrewElizabeth.teleportcommandsfabric.storage.player;

import net.minecraft.nbt.CompoundTag;

public final class PlayerProfileTestAccess {
	private PlayerProfileTestAccess() {
	}

	public static CompoundTag toNbt(PlayerProfile profile) {
		return PlayerProfileNbtCodec.toNbt(profile);
	}

	public static PlayerProfile fromNbt(CompoundTag tag) {
		return PlayerProfileNbtCodec.fromNbt(tag);
	}

	public static boolean prepareLoaded(PlayerProfile profile) {
		return PlayerProfileLifecycle.prepareLoaded(profile);
	}
}
