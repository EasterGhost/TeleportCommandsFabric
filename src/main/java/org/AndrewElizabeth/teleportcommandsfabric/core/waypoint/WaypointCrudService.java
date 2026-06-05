package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class WaypointCrudService {

	private WaypointCrudService() {
	}

	public static CompletableFuture<List<NamedLocationView>> getAll(AsyncWaypointSource source) {
		return source.getAll();
	}

	public static CompletableFuture<Optional<NamedLocationView>> getByName(String name, AsyncWaypointSource source) {
		return source.getByName(name);
	}

	public static CompletableFuture<WaypointOperationResult> add(ServerPlayer player, String name, AsyncWaypointSource source) {
		int px = player.getBlockX();
		double py = player.getY();
		int pz = player.getBlockZ();
		ResourceKey<Level> pDim = player.level().dimension();

		return source.mutateAtomic(accessor -> {
			if (accessor.findByName(name).isPresent()) {
				return WaypointOperationResult.ALREADY_EXISTS;
			}

			int maxLimit = source.getMaxLimit();
			if (maxLimit > 0 && accessor.getCount() >= maxLimit) {
				return WaypointOperationResult.LIMIT_REACHED;
			}

			NamedLocation newLoc = source.createLocation(name, px, py, pz, pDim);
			if (!accessor.put(newLoc)) {
				return WaypointOperationResult.INTERNAL_ERROR;
			}

			if (source.isDefaultSupported() && !accessor.hasDefault() && !newLoc.isTemporary()) {
				if (!accessor.setDefault(newLoc)) {
					return WaypointOperationResult.INTERNAL_ERROR;
				}
			}

			return WaypointOperationResult.SUCCESS;
		});
	}

	public static CompletableFuture<WaypointOperationResult> addTemporary(ServerPlayer player, String name, long expiredTime, AsyncWaypointSource source) {
		if (!source.isTemporarySupported()) {
			return CompletableFuture.completedFuture(WaypointOperationResult.TEMPORARY_NOT_SUPPORTED);
		}

		if (expiredTime <= System.currentTimeMillis()) {
			return CompletableFuture.completedFuture(WaypointOperationResult.INVALID_EXPIRED_TIME);
		}

		int px = player.getBlockX();
		double py = player.getY();
		int pz = player.getBlockZ();
		ResourceKey<Level> pDim = player.level().dimension();

		return source.mutateAtomic(accessor -> {
			if (accessor.hasTemporary()) {
				return WaypointOperationResult.TEMP_HOME_EXISTS;
			}

			int maxLimit = source.getMaxLimit();
			if (maxLimit > 0 && accessor.getCount() >= maxLimit) {
				return WaypointOperationResult.LIMIT_REACHED;
			}

			if (accessor.findByName(name).isPresent()) {
				return WaypointOperationResult.ALREADY_EXISTS;
			}

			NamedLocation newLoc = source.createTemporaryLocation(name, px, py, pz, pDim, expiredTime);
			if (!accessor.put(newLoc)) {
				return WaypointOperationResult.INTERNAL_ERROR;
			}

			return WaypointOperationResult.SUCCESS;
		});
	}

	public static CompletableFuture<WaypointOperationResult> delete(String name, AsyncWaypointSource source) {
		return source.mutateAtomic(accessor -> {
			Optional<NamedLocation> locationOpt = accessor.findByName(name);
			if (locationOpt.isEmpty()) {
				return WaypointOperationResult.NOT_FOUND;
			}

			accessor.remove(locationOpt.get());
			return WaypointOperationResult.SUCCESS;
		});
	}

	public static CompletableFuture<WaypointOperationResult> update(ServerPlayer player, String name, AsyncWaypointSource source) {
		net.minecraft.core.BlockPos currentPos = player.blockPosition();
		double currentY = player.getY();
		ResourceKey<Level> currentDim = player.level().dimension();

		return source.mutateAtomic(accessor -> {
			Optional<NamedLocation> locationOpt = accessor.findByName(name);
			if (locationOpt.isEmpty()) {
				return WaypointOperationResult.NOT_FOUND;
			}

			NamedLocation location = locationOpt.get();

			if (currentPos.getX() == location.getX() &&
				Double.compare(currentY, location.getYPrecise()) == 0 &&
				currentPos.getZ() == location.getZ() &&
				currentDim.equals(location.getDimension())) {
				return WaypointOperationResult.SAME_LOCATION;
			}

			NamedLocation newLoc = NamedLocation.copyOf(location);
			newLoc.setCoordinates(currentPos.getX(), currentY, currentPos.getZ(), currentDim);
			if (!accessor.put(newLoc)) {
				return WaypointOperationResult.INTERNAL_ERROR;
			}

			return WaypointOperationResult.SUCCESS;
		});
	}

	public static CompletableFuture<WaypointOperationResult> rename(String oldName, String newName, AsyncWaypointSource source) {
		return source.mutateAtomic(accessor -> {
			Optional<NamedLocation> locationOpt = accessor.findByName(oldName);
			if (locationOpt.isEmpty()) {
				return WaypointOperationResult.NOT_FOUND;
			}
			NamedLocation location = locationOpt.get();

			Optional<NamedLocation> newLocationOpt = accessor.findByName(newName);
			if (newLocationOpt.isPresent()) {
				if (!newLocationOpt.get().getUuid().equals(location.getUuid())) {
					return WaypointOperationResult.ALREADY_EXISTS;
				}
			}

			if (location.getName().equals(newName)) {
				return WaypointOperationResult.SAME_NAME;
			}

			NamedLocation newLoc = NamedLocation.copyOf(location);
			newLoc.setName(newName);
			if (!accessor.put(newLoc)) {
				return WaypointOperationResult.INTERNAL_ERROR;
			}

			return WaypointOperationResult.SUCCESS;
		});
	}

	public static CompletableFuture<WaypointOperationResult> updateVisibility(String name, boolean visible, AsyncWaypointSource source) {
		return source.mutateAtomic(accessor -> {
			Optional<NamedLocation> locationOpt = accessor.findByName(name);
			if (locationOpt.isEmpty()) {
				return WaypointOperationResult.NOT_FOUND;
			}

			NamedLocation location = locationOpt.get();

			if (location.isVisible() == visible) {
				return WaypointOperationResult.SUCCESS;
			}

			NamedLocation newLoc = NamedLocation.copyOf(location);
			newLoc.setVisible(visible);
			if (!accessor.put(newLoc)) {
				return WaypointOperationResult.INTERNAL_ERROR;
			}

			return WaypointOperationResult.SUCCESS;
		});
	}

	public static CompletableFuture<WaypointOperationResult> setDefault(String name, AsyncWaypointSource source) {
		return source.mutateAtomic(accessor -> {
			if (!source.isDefaultSupported()) {
				return WaypointOperationResult.DEFAULT_NOT_SUPPORTED;
			}

			Optional<NamedLocation> locationOpt = accessor.findByName(name);
			if (locationOpt.isEmpty()) {
				return WaypointOperationResult.NOT_FOUND;
			}

			NamedLocation location = locationOpt.get();

			if (location.isTemporary()) {
				return WaypointOperationResult.CANNOT_BE_DEFAULT;
			}

			if (accessor.isDefault(location)) {
				return WaypointOperationResult.SAME_DEFAULT;
			}

			if (!accessor.setDefault(location)) {
				return WaypointOperationResult.INTERNAL_ERROR;
			}
			return WaypointOperationResult.SUCCESS;
		});
	}
}
