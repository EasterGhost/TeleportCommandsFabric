package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.UUID;

public record SharedHomeView(
		SharedHomeKey key,
		String ownerName,
		NamedLocationSnapshot location,
		boolean mapVisible,
		int subscriptionSequence) implements NamedLocationView {
	public SharedHomeView {
		Objects.requireNonNull(key, "key");
		ownerName = ownerName == null || ownerName.isBlank() ? key.ownerUuid().toString() : ownerName;
		Objects.requireNonNull(location, "location");
	}

	@Override
	public String getName() {
		return location.getName();
	}

	@Override
	public UUID getUuid() {
		return location.getUuid();
	}

	@Override
	public int getX() {
		return location.getX();
	}

	@Override
	public double getYPrecise() {
		return location.getYPrecise();
	}

	@Override
	public int getZ() {
		return location.getZ();
	}

	@Override
	public ResourceKey<Level> getDimension() {
		return location.getDimension();
	}

	@Override
	public Float getYRot() {
		return location.getYRot();
	}

	@Override
	public Float getXRot() {
		return location.getXRot();
	}

	@Override
	public boolean isVisible() {
		return mapVisible;
	}

	@Override
	public long getExpiredTime() {
		return location.getExpiredTime();
	}

	@Override
	public int getSequence() {
		return subscriptionSequence;
	}
}
