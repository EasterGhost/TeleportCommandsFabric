package org.AndrewElizabeth.teleportcommandsfabric.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class NbtFileIO {
	private NbtFileIO() {
	}

	public static void writeAtomically(Path targetFile, CompoundTag tag) throws IOException {
		Path tempFile = targetFile.resolveSibling(targetFile.getFileName() + ".tmp");

		NbtIo.write(tag, tempFile);

		try {
			Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
