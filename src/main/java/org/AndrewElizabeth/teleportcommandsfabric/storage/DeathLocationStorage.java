package org.AndrewElizabeth.teleportcommandsfabric.storage;

import org.AndrewElizabeth.teleportcommandsfabric.common.DeathLocation;
import net.minecraft.core.BlockPos;
import java.util.HashMap;
import java.util.Optional;

public class DeathLocationStorage {
	private static final HashMap<String, DeathLocation> deathLocations = new HashMap<>();

	public static Optional<DeathLocation> getDeathLocation(String uuid) {
		return Optional.ofNullable(deathLocations.get(uuid));
	}

	public static void setDeathLocation(String uuid, BlockPos pos, String world) {

		if (deathLocations.containsKey(uuid)) {
			DeathLocation deathLocation = deathLocations.get(uuid);
			deathLocation.setBlockPos(pos);
			deathLocation.setWorld(world);
		} else {
			DeathLocation deathLocation = new DeathLocation(pos, world);
			deathLocations.put(uuid, deathLocation);
		}
	}

	public static void removeDeathLocation(String uuid) {
		deathLocations.remove(uuid);
	}

	public static void clearDeathLocations() {
		deathLocations.clear();
	}
}