package org.AndrewElizabeth.teleportcommandsfabric.storage;

import org.AndrewElizabeth.teleportcommandsfabric.models.PreviousTeleportLocation;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PreviousTeleportLocationStorage {
	private static final Map<UUID, PreviousTeleportLocation> previousTeleportLocations = new ConcurrentHashMap<>();

	private PreviousTeleportLocationStorage() {
	}

	public static Optional<PreviousTeleportLocation> getPreviousTeleportLocation(UUID uuid) {
		return Optional.ofNullable(previousTeleportLocations.get(uuid));
	}

	public static void setPreviousTeleportLocation(UUID uuid, BlockPos pos, String world) {
		previousTeleportLocations.compute(uuid, (ignored, existing) -> {
			if (existing == null) {
				return new PreviousTeleportLocation(pos, world);
			}

			existing.setBlockPos(pos);
			existing.setWorld(world);
			return existing;
		});
	}

	public static void removePreviousTeleportLocation(UUID uuid) {
		previousTeleportLocations.remove(uuid);
	}

	public static void clearPreviousTeleportLocations() {
		previousTeleportLocations.clear();
	}
}
