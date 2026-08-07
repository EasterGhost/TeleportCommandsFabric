package org.AndrewElizabeth.teleportcommandsfabric.storage.player;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.Tpa;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationNbtCodec;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;

import java.util.Map;
import java.util.UUID;

final class PlayerProfileNbtCodec {
	private PlayerProfileNbtCodec() {
	}

	static CompoundTag toNbt(PlayerProfile profile) {
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

		CompoundTag trustTag = new CompoundTag();
		trustTag.putString("DefaultTpa", profile.getDefaultTpaTrust().serializedName());
		trustTag.putString("DefaultTpaHere", profile.getDefaultTpaHereTrust().serializedName());
		ListTag trustPlayers = new ListTag();
		for (Map.Entry<UUID, TpaTrustEntry> entry : profile.getTpaTrustEntries().entrySet()) {
			if (entry.getValue().isDefault()) {
				continue;
			}
			CompoundTag playerTag = new CompoundTag();
			playerTag.putIntArray("PlayerUUID", UUIDUtil.uuidToIntArray(entry.getKey()));
			playerTag.putString("Tpa", entry.getValue().tpa().serializedName());
			playerTag.putString("TpaHere", entry.getValue().tpaHere().serializedName());
			trustPlayers.add(playerTag);
		}
		trustTag.put("Players", trustPlayers);
		tag.put("TpaTrust", trustTag);

		return tag;
	}

	static PlayerProfile fromNbt(CompoundTag tag) {
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

		tag.getCompound("TpaTrust").ifPresent(trustTag -> {
			profile.setDefaultTpaTrust(Tpa.Type.TPA,
					TpaTrustDecision.fromSerialized(trustTag.getString("DefaultTpa").orElse("default")));
			profile.setDefaultTpaTrust(Tpa.Type.TPAHERE,
					TpaTrustDecision.fromSerialized(trustTag.getString("DefaultTpaHere").orElse("default")));
			ListTag trustPlayers = trustTag.getListOrEmpty("Players");
			for (int i = 0; i < trustPlayers.size(); i++) {
				final int index = i;
				CompoundTag playerTag = trustPlayers.getCompound(index)
						.orElseThrow(() -> new IllegalArgumentException("Invalid TpaTrust.Players[" + index + "]"));
				UUID trustedUuid = UUIDUtil.uuidFromIntArray(playerTag.getIntArray("PlayerUUID")
						.orElseThrow(() -> new IllegalArgumentException("Missing TpaTrust.Players[" + index + "].PlayerUUID")));
				profile.setPlayerTpaTrust(trustedUuid, Tpa.Type.TPA,
						TpaTrustDecision.fromSerialized(playerTag.getString("Tpa").orElse("default")));
				profile.setPlayerTpaTrust(trustedUuid, Tpa.Type.TPAHERE,
						TpaTrustDecision.fromSerialized(playerTag.getString("TpaHere").orElse("default")));
			}
		});

		return profile;
	}
}
