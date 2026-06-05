package org.AndrewElizabeth.teleportcommandsfabric.storage.schema;

import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public final class NamedLocationNbtCodec {
	private NamedLocationNbtCodec() {
	}

	public static CompoundTag toNbt(NamedLocation location) {
		CompoundTag tag = new CompoundTag();
		tag.putIntArray("UUID", UUIDUtil.uuidToIntArray(location.getUuid()));
		tag.putString("Name", location.getName());
		tag.putInt("X", location.getX());
		tag.putDouble("Y", location.getYPrecise());
		tag.putInt("Z", location.getZ());
		tag.putString("Dimension", location.getDimensionId());
		tag.putBoolean("Visible", location.isVisible());
		tag.putLong("ExpiredTime", location.getExpiredTime());
		tag.putInt("Sequence", location.getSequence());
		return tag;
	}

	public static NamedLocation fromNbt(CompoundTag tag) {
		UUID uuid = UUIDUtil.uuidFromIntArray(
				tag.getIntArray("UUID").orElseThrow(() -> new IllegalArgumentException("Missing NamedLocation.Uuid")));
		String name = tag.getString("Name").orElseThrow(() -> new IllegalArgumentException("Missing NamedLocation.Name"));
		int x = tag.getInt("X").orElseThrow(() -> new IllegalArgumentException("Missing NamedLocation.X"));
		double y = tag.getDouble("Y").orElseThrow(() -> new IllegalArgumentException("Missing NamedLocation.Y"));
		int z = tag.getInt("Z").orElseThrow(() -> new IllegalArgumentException("Missing NamedLocation.Z"));
		String dimensionId = tag.getString("Dimension")
				.orElseThrow(() -> new IllegalArgumentException("Missing NamedLocation.Dimension"));
		boolean visible = tag.getBooleanOr("Visible", true);
		long expiredTime = tag.getLongOr("ExpiredTime", 0L);
		int sequence = tag.getIntOr("Sequence", -1);

		return new NamedLocation(uuid, name, x, y, z,
				WorldResolver.getDimensionById(dimensionId)
						.orElseThrow(() -> new IllegalArgumentException("Invalid dimension id: " + dimensionId)),
				visible, expiredTime, sequence);
	}
}
