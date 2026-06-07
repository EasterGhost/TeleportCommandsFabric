package org.AndrewElizabeth.teleportcommandsfabric.storage.schema;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface RecordedLocationView {
	BlockPos getBlockPos();

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
}
