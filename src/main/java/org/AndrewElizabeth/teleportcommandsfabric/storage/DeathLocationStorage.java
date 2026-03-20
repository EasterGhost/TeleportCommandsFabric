package org.AndrewElizabeth.teleportcommandsfabric.storage;

import org.AndrewElizabeth.teleportcommandsfabric.common.DeathLocation;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DeathLocationStorage {
	private static final Map<String, DeathLocation> deathLocations = new ConcurrentHashMap<>();

	private DeathLocationStorage() {
	}

	public static Optional<DeathLocation> getDeathLocation(String uuid) {
		return Optional.ofNullable(deathLocations.get(uuid));
	}

	public static void setDeathLocation(String uuid, BlockPos pos, String world) {
		deathLocations.compute(uuid, (ignored, existing) -> {
			if (existing == null) {
				return new DeathLocation(pos, world);
			}
			existing.setBlockPos(pos);
			existing.setWorld(world);
			return existing;
		});
	}

	public static void removeDeathLocation(String uuid) {
		deathLocations.remove(uuid);
	}

	public static void clearDeathLocations() {
		deathLocations.clear();
	}
}
