package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
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

public class PlayerHomeSource implements AsyncWaypointSource {
	private final UUID playerUuid;
	private final PlayerProfileManager profileManager;
	private final IntSupplier maxLimitProvider;

	public PlayerHomeSource(UUID playerUuid, PlayerProfileManager profileManager, IntSupplier maxLimitProvider) {
		this.playerUuid = playerUuid;
		this.profileManager = profileManager;
		this.maxLimitProvider = maxLimitProvider;
	}

	@Override
	public int getMaxLimit() {
		return maxLimitProvider.getAsInt();
	}

	@Override
	public CompletableFuture<WaypointOperationResult> mutateAtomic(Function<WaypointProfileAccessor, WaypointOperationResult> action) {
		return profileManager.mutate(playerUuid, profile -> action.apply(new WaypointProfileAccessor() {
			@Override
			public int getCount() {
				return profile.getHomeCount();
			}

			@Override
			public Optional<NamedLocation> findByName(String name) {
				return profile.getHomeByName(name);
			}

			@Override
			public Optional<NamedLocation> findByUuid(UUID uuid) {
				return profile.getHome(uuid);
			}

			@Override
			public boolean put(NamedLocation location) {
				if (profile.updateHome(location)) {
					return true;
				}
				return profile.addHome(location);
			}

			@Override
			public void remove(NamedLocation location) {
				profile.deleteHome(location);
			}

			@Override
			public boolean isDefault(NamedLocation location) {
				return location != null && location.getUuid().equals(profile.getDefaultHomeUuid());
			}

			@Override
			public boolean hasDefault() {
				return profile.getDefaultHomeLocation().isPresent();
			}

			@Override
			public boolean setDefault(NamedLocation location) {
				if (location != null) {
					return profile.setDefaultHome(location.getUuid());
				}
				return false;
			}

			@Override
			public boolean hasTemporary() {
				return profile.hasTemporaryHome();
			}
		}));
	}

	@Override
	public CompletableFuture<List<NamedLocationView>> getAll() {
		return profileManager.query(playerUuid, profile -> profile.getHomes());
	}

	@Override
	public CompletableFuture<Optional<NamedLocationView>> getByName(String name) {
		return profileManager.query(playerUuid, profile -> profile.getHomeByName(name));
	}

	@Override
	public NamedLocation createLocation(String name, int x, double y, int z, ResourceKey<Level> dimension) {
		return NamedLocation.create(name, x, y, z, dimension);
	}

	@Override
	public boolean isTemporarySupported() {
		return true;
	}

	@Override
	public boolean isDefaultSupported() {
		return true;
	}
}
