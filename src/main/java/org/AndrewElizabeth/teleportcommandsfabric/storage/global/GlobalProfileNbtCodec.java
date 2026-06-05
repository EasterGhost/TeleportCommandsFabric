package org.AndrewElizabeth.teleportcommandsfabric.storage.global;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationNbtCodec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class GlobalProfileNbtCodec {
	public static CompoundTag toNbt(GlobalProfile profile) {
		CompoundTag tag = new CompoundTag();

		tag.putInt("DataVersion", ModConstants.STORAGE_VERSION);
		ListTag warpList = new ListTag();
		for (NamedLocation warp : profile.getWarps()) {
			warpList.add(NamedLocationNbtCodec.toNbt(warp));
		}
		tag.put("Warps", warpList);

		return tag;
	}

	public static GlobalProfile fromNbt(CompoundTag tag) {
		GlobalProfile profile = new GlobalProfile();

		ListTag warpList = tag.getListOrEmpty("Warps");
		for (int i = 0; i < warpList.size(); i++) {
			final int index = i;
			CompoundTag warpTag = warpList.getCompound(index)
					.orElseThrow(() -> new IllegalArgumentException("Invalid Warps[" + index + "]"));
			profile.addWarp(NamedLocationNbtCodec.fromNbt(warpTag));
		}

		return profile;
	}
}
