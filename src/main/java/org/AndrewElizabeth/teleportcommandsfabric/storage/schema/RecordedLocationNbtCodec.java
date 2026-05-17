package org.AndrewElizabeth.teleportcommandsfabric.storage.schema;

import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public final class RecordedLocationNbtCodec {
	private RecordedLocationNbtCodec() {
	}

	public static CompoundTag toNbt(RecordedLocation location) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("X", location.getBlockPos().getX());
		tag.putInt("Y", location.getBlockPos().getY());
		tag.putInt("Z", location.getBlockPos().getZ());
		tag.putString("Dimension", location.getDimensionId());
		return tag;
	}

	public static RecordedLocation fromNbt(CompoundTag tag) {
		int x = tag.getInt("X").orElseThrow(() -> new IllegalArgumentException("Missing RecordedLocation.X"));
		int y = tag.getInt("Y").orElseThrow(() -> new IllegalArgumentException("Missing RecordedLocation.Y"));
		int z = tag.getInt("Z").orElseThrow(() -> new IllegalArgumentException("Missing RecordedLocation.Z"));
		String dimensionId = tag.getString("Dimension")
				.orElseThrow(() -> new IllegalArgumentException("Missing RecordedLocation.Dimension"));

		return new RecordedLocation(new BlockPos(x, y, z),
				WorldResolver.getDimensionById(dimensionId)
						.orElseThrow(() -> new IllegalArgumentException("Invalid dimension id: " + dimensionId)));
	}
}
