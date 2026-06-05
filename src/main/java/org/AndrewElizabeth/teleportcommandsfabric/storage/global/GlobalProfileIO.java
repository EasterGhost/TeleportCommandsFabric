package org.AndrewElizabeth.teleportcommandsfabric.storage.global;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.storage.NbtFileIO;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ProfileDiskIoLimiter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class GlobalProfileIO {

	public Optional<GlobalProfile> load() throws IOException {
		return ProfileDiskIoLimiter.run(() -> {
			Path globalFile = getGlobalFile();
			if (Files.notExists(globalFile)) {
				return Optional.empty();
			}

			CompoundTag tag = NbtIo.read(globalFile);
			if (tag == null) {
				throw new IOException("Global profile NBT is empty: " + globalFile);
			}

			return Optional.of(GlobalProfileNbtCodec.fromNbt(tag));
		});
	}

	public void save(GlobalProfile profile) throws IOException {
		ProfileDiskIoLimiter.run(() -> {
			Path globalFile = getGlobalFileForWrite();
			CompoundTag tag = GlobalProfileNbtCodec.toNbt(profile);
			NbtFileIO.writeAtomically(globalFile, tag);
			return null;
		});
	}

	public boolean delete() throws IOException {
		return ProfileDiskIoLimiter.run(() -> Files.deleteIfExists(getGlobalFile()));
	}

	private Path getGlobalFile() throws IOException {
		return getStorageDirectory().resolve("global.dat");
	}

	private Path getGlobalFileForWrite() throws IOException {
		Path storageDirectory = getStorageDirectory();
		Files.createDirectories(storageDirectory);
		return storageDirectory.resolve("global.dat");
	}

	private Path getStorageDirectory() throws IOException {
		if (TeleportCommands.SAVE_DIR == null) {
			throw new IOException("SAVE_DIR is not initialized");
		}

		return TeleportCommands.SAVE_DIR.resolve("TeleportCommands");
	}
}
