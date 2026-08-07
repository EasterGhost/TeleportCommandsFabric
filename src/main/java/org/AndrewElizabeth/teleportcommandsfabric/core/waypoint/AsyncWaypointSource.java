package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface AsyncWaypointSource {
	int getMaxLimit();

	CompletableFuture<WaypointOperationResult> mutateAtomic(Function<WaypointProfileAccessor, WaypointOperationResult> action);

	CompletableFuture<List<NamedLocationView>> getAll();

	CompletableFuture<Optional<NamedLocationView>> getByName(String name);

	NamedLocation createLocation(String name, int x, double y, int z, ResourceKey<Level> dimension);

	default NamedLocation createLocation(String name, int x, double y, int z, ResourceKey<Level> dimension,
			float yRot, float xRot) {
		NamedLocation location = createLocation(name, x, y, z, dimension);
		location.setRotation(yRot, xRot);
		return location;
	}

	default NamedLocation createTemporaryLocation(String name, int x, double y, int z, ResourceKey<Level> dimension, long expiredTime) {
		if (!isTemporarySupported()) {
			throw new UnsupportedOperationException("Temporary waypoint is not supported by this source");
		}
		return NamedLocation.createTemporary(name, x, y, z, dimension, expiredTime);
	}

	default NamedLocation createTemporaryLocation(String name, int x, double y, int z, ResourceKey<Level> dimension,
			long expiredTime, float yRot, float xRot) {
		NamedLocation location = createTemporaryLocation(name, x, y, z, dimension, expiredTime);
		location.setRotation(yRot, xRot);
		return location;
	}

	default boolean isTemporarySupported() {
		return false;
	}

	default boolean isDefaultSupported() {
		return false;
	}
}
