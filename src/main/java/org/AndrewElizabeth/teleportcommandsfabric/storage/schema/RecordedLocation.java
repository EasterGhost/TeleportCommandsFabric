package org.AndrewElizabeth.teleportcommandsfabric.storage.schema;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class RecordedLocation implements RecordedLocationView {
	private BlockPos pos;
	private ResourceKey<Level> dimension;
	private Float yRot;
	private Float xRot;

	public RecordedLocation(BlockPos pos, ResourceKey<Level> dimension) {
		this(pos, dimension, null, null);
	}

	public RecordedLocation(BlockPos pos, ResourceKey<Level> dimension, Float yRot, Float xRot) {
		this.pos = pos;
		this.dimension = dimension;
		setRotation(yRot, xRot);
	}

	public static RecordedLocation copyOf(RecordedLocationView location) {
		return new RecordedLocation(location.getBlockPos(), location.getDimension(), location.getYRot(), location.getXRot());
	}

	@Override
	public BlockPos getBlockPos() {
		return pos;
	}

	@Override
	public ResourceKey<Level> getDimension() {
		return dimension;
	}

	@Override
	public Float getYRot() {
		return yRot;
	}

	@Override
	public Float getXRot() {
		return xRot;
	}

	public void setBlockPos(BlockPos pos) {
		this.pos = pos;
	}

	public void setDimension(ResourceKey<Level> dimension) {
		this.dimension = dimension;
	}

	public void setRotation(Float yRot, Float xRot) {
		if ((yRot == null) != (xRot == null)) {
			this.yRot = null;
			this.xRot = null;
			return;
		}
		this.yRot = yRot;
		this.xRot = xRot;
	}
}
