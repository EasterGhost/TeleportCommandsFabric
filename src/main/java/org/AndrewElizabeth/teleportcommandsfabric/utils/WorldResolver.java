package org.AndrewElizabeth.teleportcommandsfabric.utils;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

public final class WorldResolver {

	private WorldResolver() {
	}

	public static String getDimensionId(ResourceKey<Level> dimensionKey) {
		// ResourceKey#location() is unavailable in this mapping; normalize from toString.
		String raw = dimensionKey.toString();
		int splitIndex = raw.indexOf("/ ");
		if (splitIndex >= 0 && raw.endsWith("]")) {
			return raw.substring(splitIndex + 2, raw.length() - 1);
		}
		return raw;
	}

	public static Optional<ServerLevel> getDimensionById(String worldId) {
		if (TeleportCommands.SERVER == null || worldId == null || worldId.isBlank()) {
			return Optional.empty();
		}
		return StreamSupport.stream(TeleportCommands.SERVER.getAllLevels().spliterator(), false)
				.filter(level -> Objects.equals(getDimensionId(level.dimension()), worldId))
				.findFirst();
	}
}
