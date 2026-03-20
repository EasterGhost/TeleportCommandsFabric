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
		return dimensionKey.identifier().toString();
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
