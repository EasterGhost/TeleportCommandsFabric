package org.AndrewElizabeth.teleportcommandsfabric.storage.player;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationNbtCodec;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;

import java.util.UUID;

public final class PlayerProfileNbtCodec {
	public static CompoundTag toNbt(PlayerProfile profile) {
		CompoundTag tag = new CompoundTag();

		tag.putInt("DataVersion", ModConstants.STORAGE_VERSION);
		tag.putIntArray("PlayerUUID", UUIDUtil.uuidToIntArray(profile.getPlayerUuid()));

		if (profile.getDefaultHomeUuid() != null) {
			tag.putIntArray("DefaultHomeUUID", UUIDUtil.uuidToIntArray(profile.getDefaultHomeUuid()));
		}

		ListTag homeList = new ListTag();
		for (NamedLocation home : profile.getHomes()) {
			homeList.add(NamedLocationNbtCodec.toNbt(home));
		}
		tag.put("Homes", homeList);

		ListTag hiddenWarpList = new ListTag();
		for (UUID warpUuid : profile.getHiddenWarpUuids()) {
			hiddenWarpList.add(new IntArrayTag(UUIDUtil.uuidToIntArray(warpUuid)));
		}
		tag.put("HiddenWarpUUIDs", hiddenWarpList);

		return tag;
	}

	public static PlayerProfile fromNbt(CompoundTag tag) {
		UUID playerUuid = UUIDUtil.uuidFromIntArray(
				tag.getIntArray("PlayerUUID").orElseThrow(() -> new IllegalArgumentException("Missing PlayerUUID")));
		PlayerProfile profile = new PlayerProfile(playerUuid);

		ListTag homeList = tag.getListOrEmpty("Homes");
		for (int i = 0; i < homeList.size(); i++) {
			final int index = i;
			CompoundTag homeTag = homeList.getCompound(index)
					.orElseThrow(() -> new IllegalArgumentException("Invalid Homes[" + index + "]"));
			profile.addHome(NamedLocationNbtCodec.fromNbt(homeTag));
		}

		tag.getIntArray("DefaultHomeUUID")
				.map(UUIDUtil::uuidFromIntArray)
				.ifPresent(profile::setDefaultHome);

		ListTag hiddenWarpList = tag.getListOrEmpty("HiddenWarpUUIDs");
		for (int i = 0; i < hiddenWarpList.size(); i++) {
			final int index = i;
			UUID warpUuid = UUIDUtil.uuidFromIntArray(hiddenWarpList.getIntArray(index)
					.orElseThrow(() -> new IllegalArgumentException("Invalid HiddenWarpUUIDs[" + index + "]")));
			profile.hideWarp(warpUuid);
		}

		return profile;
	}
}
