package org.AndrewElizabeth.teleportcommandsfabric.common;

import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager;
import net.minecraft.core.BlockPos;

public class NamedLocation implements WorldLocated {
	private String name;
	private final int x;
	private final double y;
	private final int z;
	private final String world;

	public NamedLocation(String name, int x, double y, int z, String world) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.world = world;
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
	// -----

	public void setName(String name) throws Exception {
		this.name = name;
		StorageManager.StorageSaver();
	}

}
