package org.AndrewElizabeth.teleportcommandsfabric.storage.schema;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class RecordedLocation implements RecordedLocationView {
	private BlockPos pos;
	private ResourceKey<Level> dimension;

	public RecordedLocation(BlockPos pos, ResourceKey<Level> dimension) {
		this.pos = pos;
		this.dimension = dimension;
	}

	public static RecordedLocation copyOf(RecordedLocationView location) {
		return new RecordedLocation(location.getBlockPos(), location.getDimension());
	}

	@Override
	public BlockPos getBlockPos() {
		return pos;
	}

	@Override
	public ResourceKey<Level> getDimension() {
		return dimension;
	}

	public void setBlockPos(BlockPos pos) {
		this.pos = pos;
	}

	public void setDimension(ResourceKey<Level> dimension) {
		this.dimension = dimension;
	}
}
