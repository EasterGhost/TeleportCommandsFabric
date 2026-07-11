package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WaypointCrudServiceTest {
	private static final ResourceKey<Level> OVERWORLD = dimension("overworld");
	private static final ResourceKey<Level> NETHER = dimension("the_nether");

	@Test
	void conditionalDeleteRemovesUnchangedWaypoint() {
		NamedLocation location = location(UUID.randomUUID(), "home", OVERWORLD);
		InMemorySource source = new InMemorySource(location);

		WaypointOperationResult result = WaypointCrudService
				.deleteIfUnchanged(NamedLocationSnapshot.from(location), source)
				.join();

		assertEquals(WaypointOperationResult.SUCCESS, result);
		assertTrue(source.current().isEmpty());
	}

	@Test
	void conditionalDeletePreservesSameNameReplacement() {
		NamedLocation original = location(UUID.randomUUID(), "home", OVERWORLD);
		NamedLocation replacement = location(UUID.randomUUID(), "home", NETHER);
		InMemorySource source = new InMemorySource(replacement);

		WaypointOperationResult result = WaypointCrudService
				.deleteIfUnchanged(NamedLocationSnapshot.from(original), source)
				.join();

		assertEquals(WaypointOperationResult.NOT_FOUND, result);
		assertSame(replacement, source.current().orElseThrow());
	}

	@Test
	void conditionalDeletePreservesUpdatedWaypoint() {
		NamedLocation original = location(UUID.randomUUID(), "home", OVERWORLD);
		NamedLocation updated = NamedLocation.copyOf(original);
		updated.setCoordinates(100, 80.5D, -40, NETHER);
		InMemorySource source = new InMemorySource(updated);

		WaypointOperationResult result = WaypointCrudService
				.deleteIfUnchanged(NamedLocationSnapshot.from(original), source)
				.join();

		assertEquals(WaypointOperationResult.NOT_FOUND, result);
		assertSame(updated, source.current().orElseThrow());
	}

	private static NamedLocation location(UUID uuid, String name, ResourceKey<Level> dimension) {
		return new NamedLocation(uuid, name, 10, 64.0D, 20, dimension, true);
	}

	private static ResourceKey<Level> dimension(String path) {
		return ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("minecraft", path));
	}

	private static final class InMemorySource implements AsyncWaypointSource, WaypointProfileAccessor {
		private NamedLocation current;

		private InMemorySource(NamedLocation current) {
			this.current = current;
		}

		private Optional<NamedLocation> current() {
			return Optional.ofNullable(current);
		}

		@Override
		public int getMaxLimit() {
			return 0;
		}

		@Override
		public CompletableFuture<WaypointOperationResult> mutateAtomic(
				Function<WaypointProfileAccessor, WaypointOperationResult> action) {
			return CompletableFuture.completedFuture(action.apply(this));
		}

		@Override
		public int getCount() {
			return current == null ? 0 : 1;
		}

		@Override
		public Optional<NamedLocation> findByName(String name) {
			return current != null && current.getName().equals(name)
					? Optional.of(current)
					: Optional.empty();
		}

		@Override
		public Optional<NamedLocation> findByUuid(UUID uuid) {
			return current != null && current.getUuid().equals(uuid)
					? Optional.of(current)
					: Optional.empty();
		}

		@Override
		public boolean put(NamedLocation location) {
			current = location;
			return true;
		}

		@Override
		public void remove(NamedLocation location) {
			if (current == location) {
				current = null;
			}
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

		@Override
		public CompletableFuture<List<NamedLocationView>> getAll() {
			return CompletableFuture.completedFuture(current == null ? List.of() : List.of(current));
		}

		@Override
		public CompletableFuture<Optional<NamedLocationView>> getByName(String name) {
			return CompletableFuture.completedFuture(current()
					.filter(location -> location.getName().equals(name))
					.map(NamedLocationView.class::cast));
		}

		@Override
		public NamedLocation createLocation(String name, int x, double y, int z, ResourceKey<Level> dimension) {
			return NamedLocation.create(name, x, y, z, dimension);
		}
	}
}
