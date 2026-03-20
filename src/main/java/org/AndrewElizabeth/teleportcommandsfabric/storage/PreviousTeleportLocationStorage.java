package org.AndrewElizabeth.teleportcommandsfabric.storage;

import org.AndrewElizabeth.teleportcommandsfabric.common.PreviousTeleportLocation;

import net.minecraft.core.BlockPos;

import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PreviousTeleportLocationStorage {
	private static final Map<UUID, PreviousTeleportLocation> previousTeleportLocations = new ConcurrentHashMap<>();

	private PreviousTeleportLocationStorage() {
	}

	public static Optional<PreviousTeleportLocation> getPreviousTeleportLocation(UUID uuid) {
		return Optional.ofNullable(previousTeleportLocations.get(uuid));
	}

	public static void setPreviousTeleportLocation(UUID uuid, BlockPos pos, String world) {
		if (previousTeleportLocations.containsKey(uuid)) {
			PreviousTeleportLocation previousTeleportLocation = previousTeleportLocations.get(uuid);
			previousTeleportLocation.setBlockPos(pos);
			previousTeleportLocation.setWorld(world);
		} else {
			previousTeleportLocations.put(uuid, new PreviousTeleportLocation(pos, world));
		}
	}

	public static void removePreviousTeleportLocation(UUID uuid) {
		previousTeleportLocations.remove(uuid);
	}

	public static void clearPreviousTeleportLocations() {
		previousTeleportLocations.clear();
	}
}
