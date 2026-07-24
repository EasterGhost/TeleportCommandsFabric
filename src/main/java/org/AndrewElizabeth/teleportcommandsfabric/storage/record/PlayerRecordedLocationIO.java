package org.AndrewElizabeth.teleportcommandsfabric.storage.record;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.storage.NbtFileIO;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

final class PlayerRecordedLocationIO {
	Map<UUID, PlayerRecordedLocations> loadRecords() throws IOException {
		Path recordFile = getRecordFile();
		if (Files.notExists(recordFile)) {
			return Collections.emptyMap();
		}

		CompoundTag tag = NbtIo.read(recordFile);
		if (tag == null) {
			throw new IOException("Recorded location NBT is empty: " + recordFile);
		}

		return PlayerRecordedLocationNbtCodec.fromNbt(tag);
	}

	void saveRecords(Map<UUID, PlayerRecordedLocations> records) throws IOException {
		if (records.isEmpty()) {
			delete();
			return;
		}

		Path recordFile = getRecordFileForWrite();
		CompoundTag tag = PlayerRecordedLocationNbtCodec.toNbt(records);
		NbtFileIO.writeAtomically(recordFile, tag);
	}

	boolean delete() throws IOException {
		return Files.deleteIfExists(getRecordFile());
	}

	private Path getRecordFile() throws IOException {
		return getStorageDirectory().resolve("recorded_locations.dat");
	}

	private Path getRecordFileForWrite() throws IOException {
		Path storageDirectory = getStorageDirectory();
		Files.createDirectories(storageDirectory);
		return storageDirectory.resolve("recorded_locations.dat");
	}

	private Path getStorageDirectory() throws IOException {
		if (TeleportCommands.SAVE_DIR == null) {
			throw new IOException("SAVE_DIR is not initialized");
		}

		return TeleportCommands.SAVE_DIR.resolve("TeleportCommands");
	}
}
