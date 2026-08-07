package org.AndrewElizabeth.teleportcommandsfabric.storage.schema;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

public record RecordedLocationSnapshot(BlockPos getBlockPos, ResourceKey<Level> getDimension,
		Float getYRot, Float getXRot) implements RecordedLocationView {
	public static RecordedLocationSnapshot from(RecordedLocationView location) {
		return new RecordedLocationSnapshot(location.getBlockPos(), location.getDimension(), location.getYRot(), location.getXRot());
	}

	public static Optional<RecordedLocationView> optional(Optional<? extends RecordedLocationView> location) {
		return location.map(RecordedLocationSnapshot::from);
	}
}
