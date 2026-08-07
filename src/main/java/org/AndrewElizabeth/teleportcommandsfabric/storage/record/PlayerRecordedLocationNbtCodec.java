package org.AndrewElizabeth.teleportcommandsfabric.storage.record;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocationNbtCodec;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class PlayerRecordedLocationNbtCodec {
	private PlayerRecordedLocationNbtCodec() {
	}

	static CompoundTag toNbt(Map<UUID, PlayerRecordedLocations> records) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("DataVersion", ModConstants.STORAGE_VERSION);
		ListTag recordList = new ListTag();

		for (Map.Entry<UUID, PlayerRecordedLocations> entry : records.entrySet()) {
			CompoundTag recordTag = new CompoundTag();
			recordTag.putIntArray("PlayerUUID", UUIDUtil.uuidToIntArray(entry.getKey()));
			entry.getValue().getDeathLocation().ifPresent(location -> recordTag.put("DeathLocation", RecordedLocationNbtCodec.toNbt(location)));
			entry.getValue().getPreviousTeleportLocation()
					.ifPresent(location -> recordTag.put("PreviousTeleportLocation", RecordedLocationNbtCodec.toNbt(location)));
			recordList.add(recordTag);
		}

		tag.put("PlayerBackRecords", recordList);
		return tag;
	}

	static Map<UUID, PlayerRecordedLocations> fromNbt(CompoundTag tag) {
		Map<UUID, PlayerRecordedLocations> records = new LinkedHashMap<>();
		ListTag recordList = tag.getListOrEmpty("PlayerBackRecords");

		for (int i = 0; i < recordList.size(); i++) {
			final int index = i;
			CompoundTag recordTag = recordList.getCompound(index)
					.orElseThrow(() -> new IllegalArgumentException("Invalid PlayerBackRecords[" + index + "]"));
			UUID playerUuid = UUIDUtil.uuidFromIntArray(recordTag.getIntArray("PlayerUUID")
					.orElseThrow(() -> new IllegalArgumentException("Missing PlayerBackRecords[" + index + "].PlayerUUID")));

			PlayerRecordedLocations record = new PlayerRecordedLocations();
			recordTag.getCompound("DeathLocation")
					.map(RecordedLocationNbtCodec::fromNbt)
					.ifPresent(record::setDeathLocation);
			recordTag.getCompound("PreviousTeleportLocation")
					.map(RecordedLocationNbtCodec::fromNbt)
					.ifPresent(record::setPreviousTeleportLocation);

			if (!record.isEmpty()) {
				records.put(playerUuid, record);
			}
		}

		return records;
	}
}
