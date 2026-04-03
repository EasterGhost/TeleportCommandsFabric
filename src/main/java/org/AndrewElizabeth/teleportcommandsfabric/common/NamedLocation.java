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
	private int x;
	private double y;
	private int z;
	private String world;
	private boolean xaeroVisible;
	private long expiredTime;

	public NamedLocation(UUID uuid, String name, int x, double y, int z, String world, boolean xaeroVisible, long expiredTime) {
		this.uuid = normalizeUuid(uuid);
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.world = world;
		this.xaeroVisible = xaeroVisible;
		this.expiredTime = expiredTime;
	}

	public NamedLocation(UUID uuid, String name, int x, double y, int z, String world, boolean xaeroVisible) {
		this(uuid, name, x, y, z, world, xaeroVisible, 0L);
	}

	public static NamedLocation create(String name, int x, double y, int z, String world) {
		return new NamedLocation(UUID.randomUUID(), name, x, y, z, world, true, 0L);
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
		StorageManager.markDirty();
	}

	public void setCoordinates(int x, double y, int z, String world) throws Exception {
		this.x = x;
		this.y = y;
		this.z = z;
		this.world = world;
		StorageManager.markDirty();
	}

	public void setXaeroVisible(boolean xaeroVisible) throws Exception {
		this.xaeroVisible = xaeroVisible;
		StorageManager.markDirty();
	}

	public long getExpiredTime() {
		return expiredTime;
	}

	public void setExpiredTime(long expiredTime) throws Exception {
		this.expiredTime = expiredTime;
		StorageManager.markDirty();
	}

	public boolean isExpired() {
		return expiredTime > 0 && System.currentTimeMillis() > expiredTime;
	}

	public boolean ensureUuid() {
		if (uuid != null) {
			return false;
		}
		uuid = UUID.randomUUID();
		return true;
	}

	private static UUID normalizeUuid(UUID uuid) {
		return uuid == null ? UUID.randomUUID() : uuid;
	}
}
