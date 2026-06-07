package org.AndrewElizabeth.teleportcommandsfabric.storage.schema;

import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public final class RecordedLocationNbtCodec {
	private RecordedLocationNbtCodec() {
	}

	private static final String Y_ROT_KEY = "YRot";
	private static final String X_ROT_KEY = "XRot";

	public static CompoundTag toNbt(RecordedLocation location) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("X", location.getBlockPos().getX());
		tag.putInt("Y", location.getBlockPos().getY());
		tag.putInt("Z", location.getBlockPos().getZ());
		tag.putString("Dimension", location.getDimensionId());
		if (location.getYRot() != null && location.getXRot() != null) {
			tag.putFloat(Y_ROT_KEY, location.getYRot());
			tag.putFloat(X_ROT_KEY, location.getXRot());
		}
		return tag;
	}

	public static RecordedLocation fromNbt(CompoundTag tag) {
		int x = tag.getInt("X").orElseThrow(() -> new IllegalArgumentException("Missing RecordedLocation.X"));
		int y = tag.getInt("Y").orElseThrow(() -> new IllegalArgumentException("Missing RecordedLocation.Y"));
		int z = tag.getInt("Z").orElseThrow(() -> new IllegalArgumentException("Missing RecordedLocation.Z"));
		String dimensionId = tag.getString("Dimension")
				.orElseThrow(() -> new IllegalArgumentException("Missing RecordedLocation.Dimension"));
		Float yRot = tag.getFloat(Y_ROT_KEY).orElse(null);
		Float xRot = tag.getFloat(X_ROT_KEY).orElse(null);
		if ((yRot == null) != (xRot == null)) {
			yRot = null;
			xRot = null;
		}

		return new RecordedLocation(new BlockPos(x, y, z),
				WorldResolver.getDimensionById(dimensionId)
						.orElseThrow(() -> new IllegalArgumentException("Invalid dimension id: " + dimensionId)),
				yRot, xRot);
	}
}
