package org.AndrewElizabeth.teleportcommandsfabric.common;

import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.UUID;

public class NamedLocation {
	private UUID uuid;
	private String name;
	private final int x;
	private final double y;
	private final int z;
	private final String world;
	private boolean xaeroVisible;

	public NamedLocation(UUID uuid, String name, int x, double y, int z, String world, boolean xaeroVisible) {
		this.uuid = normalizeUuid(uuid);
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.world = world;
		this.xaeroVisible = xaeroVisible;
	}

	public static NamedLocation create(String name, int x, double y, int z, String world) {
		return new NamedLocation(UUID.randomUUID(), name, x, y, z, world, true);
	}

	public String getName() {
		return this.name;
	}

	public UUID getUuid() {
		return uuid;
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

	public String getWorldString() {
		return this.world;
	}

	public Optional<ServerLevel> getWorld() {
		return WorldResolver.getDimensionById(world);
	}

	public boolean isXaeroVisible() {
		return xaeroVisible;
	}

	public void setName(String name) throws Exception {
		this.name = name;
		StorageManager.StorageSaver();
	}

	public void setXaeroVisible(boolean xaeroVisible) throws Exception {
		this.xaeroVisible = xaeroVisible;
		StorageManager.StorageSaver();
	}

	public boolean ensureUuid() {
		UUID normalized = normalizeUuid(uuid);
		if (normalized.equals(uuid)) {
			return false;
		}
		uuid = normalized;
		return true;
	}

	private static UUID normalizeUuid(UUID uuid) {
		return uuid == null ? UUID.randomUUID() : uuid;
	}

}
