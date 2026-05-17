package org.AndrewElizabeth.teleportcommandsfabric.utils;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class WorldResolver {

	private WorldResolver() {
	}

	public static String getDimensionId(ResourceKey<Level> dimensionKey) {
		return dimensionKey.identifier().toString();
	}

	public static Optional<ResourceKey<Level>> getDimensionById(String worldId) {
		if (worldId == null || worldId.isBlank()) {
			return Optional.empty();
		}

		Identifier identifier = Identifier.tryParse(worldId);
		if (identifier == null) {
			return Optional.empty();
		}

		return Optional.of(ResourceKey.create(Registries.DIMENSION, identifier));
	}

	public static Optional<ServerLevel> getLevelById(String worldId) {
		if (TeleportCommands.SERVER == null) {
			return Optional.empty();
		}

		return getDimensionById(worldId)
				.map(TeleportCommands.SERVER::getLevel);
	}

	public static Optional<ServerLevel> getLevel(ResourceKey<Level> dimensionKey) {
		if (TeleportCommands.SERVER == null || dimensionKey == null) {
			return Optional.empty();
		}

		return Optional.ofNullable(TeleportCommands.SERVER.getLevel(dimensionKey));
	}
}
