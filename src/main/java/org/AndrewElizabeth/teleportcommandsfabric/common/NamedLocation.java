package org.AndrewElizabeth.teleportcommandsfabric.common;

import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public class NamedLocation {
	private String name;
	private final int x;
	private final double y;
	private final int z;
	private final String world;
	private boolean xaeroVisible;

	public NamedLocation(String name, int x, double y, int z, String world) {
		this(name, x, y, z, world, true);
	}

	public NamedLocation(String name, int x, double y, int z, String world, boolean xaeroVisible) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.world = world;
		this.xaeroVisible = xaeroVisible;
	}

	// -----

	public String getName() {
		return this.name;
	}

	public BlockPos getBlockPos() {
		return new BlockPos(this.x, (int) Math.floor(this.y), this.z);
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return (int) Math.floor(this.y);
	}

	public double getYPrecise() {
		return this.y;
	}

	public int getZ() {
		return this.z;
	}

	// Return the world id as a string
	public String getWorldString() {
		return this.world;
	}

	public Optional<ServerLevel> getWorld() {
		return WorldResolver.getDimensionById(world);
	}

	public boolean isXaeroVisible() {
		return xaeroVisible;
	}

	// -----

	public void setName(String name) throws Exception {
		this.name = name;
		StorageManager.StorageSaver();
	}

	public void setXaeroVisible(boolean xaeroVisible) throws Exception {
		this.xaeroVisible = xaeroVisible;
		StorageManager.StorageSaver();
	}

}
