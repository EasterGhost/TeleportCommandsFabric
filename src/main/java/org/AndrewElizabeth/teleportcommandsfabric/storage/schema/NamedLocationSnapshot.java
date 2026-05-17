package org.AndrewElizabeth.teleportcommandsfabric.storage.schema;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record NamedLocationSnapshot(UUID getUuid, String getName, int getX, double getYPrecise, int getZ,
		ResourceKey<Level> getDimension, boolean isVisible, long getExpiredTime, int getSequence) implements NamedLocationView {
	public static NamedLocationSnapshot from(NamedLocationView location) {
		return new NamedLocationSnapshot(location.getUuid(),
				location.getName(),
				location.getX(),
				location.getYPrecise(),
				location.getZ(),
				location.getDimension(),
				location.isVisible(),
				location.getExpiredTime(),
				location.getSequence());
	}

	public static Optional<NamedLocationView> optional(Optional<? extends NamedLocationView> location) {
		return location.map(NamedLocationSnapshot::from);
	}

	public static List<NamedLocationView> list(Collection<? extends NamedLocationView> locations) {
		return locations.stream()
				.filter(location -> !location.isExpired())
				.map(NamedLocationSnapshot::from)
				.map(NamedLocationView.class::cast)
				.toList();
	}
}
