package org.AndrewElizabeth.teleportcommandsfabric.storage.schema;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

public interface NamedLocationView {
	String getName();

	UUID getUuid();

	default BlockPos getBlockPos() {
		return new BlockPos(getX(), getY(), getZ());
	}

	int getX();

	default int getY() {
		return (int) Math.floor(getYPrecise());
	}

	double getYPrecise();

	int getZ();

	ResourceKey<Level> getDimension();

	default String getDimensionId() {
		return getDimension().identifier().toString();
	}

	default Float getYRot() {
		return null;
	}

	default Float getXRot() {
		return null;
	}

	boolean isVisible();

	long getExpiredTime();

	int getSequence();

	default boolean isTemporary() {
		return getExpiredTime() > 0;
	}

	default boolean isExpired() {
		long expiredTime = getExpiredTime();
		return expiredTime > 0 && System.currentTimeMillis() >= expiredTime;
	}
}
