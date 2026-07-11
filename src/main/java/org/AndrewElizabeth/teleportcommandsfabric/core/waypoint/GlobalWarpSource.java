package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.IntSupplier;

public class GlobalWarpSource implements AsyncWaypointSource {
	private final GlobalProfileManager profileManager;
	private final IntSupplier maxLimitProvider;

	public GlobalWarpSource(GlobalProfileManager profileManager, IntSupplier maxLimitProvider) {
		this.profileManager = profileManager;
		this.maxLimitProvider = maxLimitProvider;
	}

	@Override
	public int getMaxLimit() {
		return maxLimitProvider.getAsInt();
	}

	@Override
	public CompletableFuture<WaypointOperationResult> mutateAtomic(Function<WaypointProfileAccessor, WaypointOperationResult> action) {
		return profileManager.mutate(profile -> action.apply(new WaypointProfileAccessor() {
			@Override
			public int getCount() {
				return profile.getWarpCount();
			}

			@Override
			public Optional<NamedLocation> findByName(String name) {
				return profile.getWarpByName(name);
			}

			@Override
			public Optional<NamedLocation> findByUuid(UUID uuid) {
				return profile.getWarp(uuid);
			}

			@Override
			public boolean put(NamedLocation location) {
				if (profile.updateWarp(location)) {
					return true;
				}
				return profile.addWarp(location);
			}

			@Override
			public void remove(NamedLocation location) {
				profile.deleteWarp(location);
			}

			@Override
			public boolean isDefault(NamedLocation location) {
				return false;
			}

			@Override
			public boolean hasDefault() {
				return false;
			}

			@Override
			public boolean setDefault(NamedLocation location) {
				return false;
			}

			@Override
			public boolean hasTemporary() {
				return false;
			}
		}));
	}

	@Override
	public CompletableFuture<List<NamedLocationView>> getAll() {
		return profileManager.query(profile -> profile.getWarps());
	}

	@Override
	public CompletableFuture<Optional<NamedLocationView>> getByName(String name) {
		return profileManager.query(profile -> profile.getWarpByName(name));
	}

	@Override
	public NamedLocation createLocation(String name, int x, double y, int z, ResourceKey<Level> dimension) {
		return NamedLocation.create(name, x, y, z, dimension);
	}
}
