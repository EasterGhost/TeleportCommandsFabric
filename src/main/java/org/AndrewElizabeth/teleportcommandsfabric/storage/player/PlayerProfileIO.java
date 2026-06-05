package org.AndrewElizabeth.teleportcommandsfabric.storage.player;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ProfileDiskIoLimiter;
import org.AndrewElizabeth.teleportcommandsfabric.storage.NbtFileIO;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public class PlayerProfileIO {
	public Optional<PlayerProfile> load(UUID uuid) throws IOException {
		return ProfileDiskIoLimiter.run(() -> {
			Path playerFile = getPlayerFile(uuid);
			if (Files.notExists(playerFile)) {
				return Optional.empty();
			}

			CompoundTag tag = NbtIo.read(playerFile);
			if (tag == null) {
				throw new IOException("Player profile NBT is empty: " + playerFile);
			}

			PlayerProfile profile = PlayerProfileNbtCodec.fromNbt(tag);
			if (!uuid.equals(profile.getPlayerUuid())) {
				throw new IOException("Player profile uuid mismatch: expected " + uuid + " but got " + profile.getPlayerUuid());
			}

			return Optional.of(profile);
		});
	}

	public void save(PlayerProfile profile) throws IOException {
		ProfileDiskIoLimiter.run(() -> {
			Path playerFile = getPlayerFileForWrite(profile.getPlayerUuid());
			CompoundTag tag = PlayerProfileNbtCodec.toNbt(profile);
			NbtFileIO.writeAtomically(playerFile, tag);
			return null;
		});
	}

	public boolean delete(UUID uuid) throws IOException {
		return ProfileDiskIoLimiter.run(() -> Files.deleteIfExists(getPlayerFile(uuid)));
	}

	private Path getPlayerFile(UUID uuid) throws IOException {
		return getPlayerDirectory().resolve(uuid + ".dat");
	}

	private Path getPlayerFileForWrite(UUID uuid) throws IOException {
		Path playerDirectory = getPlayerDirectory();
		Files.createDirectories(playerDirectory);
		return playerDirectory.resolve(uuid + ".dat");
	}

	private Path getPlayerDirectory() throws IOException {
		if (TeleportCommands.SAVE_DIR == null) {
			throw new IOException("SAVE_DIR is not initialized");
		}

		return TeleportCommands.SAVE_DIR.resolve("TeleportCommands").resolve("player");
	}
}
