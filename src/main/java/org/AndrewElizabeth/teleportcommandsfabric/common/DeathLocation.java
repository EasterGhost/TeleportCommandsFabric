package org.AndrewElizabeth.teleportcommandsfabric.common;

import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public class DeathLocation {
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

	public Optional<ServerLevel> getWorld() {
		return WorldResolver.getDimensionById(world);
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
