package org.AndrewElizabeth.teleportcommandsfabric.storage.schema;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class NamedLocation implements NamedLocationView {
	private UUID uuid;
	private String name;
	private int x;
	private double y;
	private int z;
	private ResourceKey<Level> dimension;
	private Float yRot;
	private Float xRot;
	private boolean visible;
	private long expiredTime;
	private int sequence;

	public NamedLocation(UUID uuid, String name, int x, double y, int z, ResourceKey<Level> dimension,
			Float yRot, Float xRot, boolean visible, long expiredTime, int sequence) {
		this.uuid = normalizeUuid(uuid);
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
		setRotation(yRot, xRot);
		this.visible = visible;
		this.expiredTime = expiredTime;
		this.sequence = sequence;
	}

	public NamedLocation(UUID uuid, String name, int x, double y, int z, ResourceKey<Level> dimension, boolean visible, long expiredTime, int sequence) {
		this(uuid, name, x, y, z, dimension, null, null, visible, expiredTime, sequence);
	}

	public NamedLocation(UUID uuid, String name, int x, double y, int z, ResourceKey<Level> dimension, boolean visible, long expiredTime) {
		this(uuid, name, x, y, z, dimension, visible, expiredTime, -1);
	}

	public NamedLocation(UUID uuid, String name, int x, double y, int z, ResourceKey<Level> dimension, boolean visible) {
		this(uuid, name, x, y, z, dimension, visible, 0L);
	}

	public static NamedLocation create(String name, int x, double y, int z, ResourceKey<Level> dimension) {
		return new NamedLocation(UUID.randomUUID(), name, x, y, z, dimension, true, 0L);
	}

	public static NamedLocation create(String name, int x, double y, int z, ResourceKey<Level> dimension,
			float yRot, float xRot) {
		return new NamedLocation(UUID.randomUUID(), name, x, y, z, dimension, yRot, xRot, true, 0L, -1);
	}

	public static NamedLocation createTemporary(String name, int x, double y, int z, ResourceKey<Level> dimension, long expiredTime) {
		return new NamedLocation(UUID.randomUUID(), name, x, y, z, dimension, true, expiredTime);
	}

	public static NamedLocation createTemporary(String name, int x, double y, int z, ResourceKey<Level> dimension,
			long expiredTime, float yRot, float xRot) {
		return new NamedLocation(UUID.randomUUID(), name, x, y, z, dimension, yRot, xRot, true, expiredTime, -1);
	}

	public static NamedLocation copyOf(NamedLocationView location) {
		return new NamedLocation(location.getUuid(), location.getName(), location.getX(), location.getYPrecise(),
				location.getZ(), location.getDimension(), location.getYRot(), location.getXRot(),
				location.isVisible(), location.getExpiredTime(), location.getSequence());
	}

	public String getName() {
		return this.name;
	}

	public UUID getUuid() {
		return uuid;
	}

	public int getX() {
		return this.x;
	}

	public double getYPrecise() {
		return this.y;
	}

	public int getZ() {
		return this.z;
	}

	public ResourceKey<Level> getDimension() {
		return this.dimension;
	}

	public Float getYRot() {
		return yRot;
	}

	public Float getXRot() {
		return xRot;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setCoordinates(int x, double y, int z, ResourceKey<Level> dimension) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.dimension = dimension;
	}

	public void setCoordinates(int x, double y, int z, ResourceKey<Level> dimension, float yRot, float xRot) {
		setCoordinates(x, y, z, dimension);
		setRotation(yRot, xRot);
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

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public long getExpiredTime() {
		return expiredTime;
	}

	public void setExpiredTime(long expiredTime) {
		this.expiredTime = expiredTime;
	}

	public int getSequence() {
		return sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	private static UUID normalizeUuid(UUID uuid) {
		return uuid == null ? UUID.randomUUID() : uuid;
	}
}
