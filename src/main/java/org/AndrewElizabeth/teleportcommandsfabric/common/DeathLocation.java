package org.AndrewElizabeth.teleportcommandsfabric.common;

import net.minecraft.core.BlockPos;

public class DeathLocation implements WorldLocated {
	private BlockPos pos;
	private String world;

	public DeathLocation(BlockPos pos, String world) {
		this.pos = pos;
		this.world = world;
	}

	// -----

	public BlockPos getBlockPos() {
		return pos;
	}

	public String getWorldString() {
		return world;
	}
	// ----- note to self: these don't need to be saved since this class isn't a
	// part of the storage :3

	public void setBlockPos(BlockPos pos) {
		this.pos = pos;
	}

	public void setWorld(String world) {
		this.world = world;
	}
}
