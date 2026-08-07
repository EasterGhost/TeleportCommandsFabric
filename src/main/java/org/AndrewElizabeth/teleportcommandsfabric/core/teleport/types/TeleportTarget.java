package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public record TeleportTarget(ServerLevel world, Vec3 position, Float yRot, Float xRot) {
	public TeleportTarget(ServerLevel world, Vec3 position) {
		this(world, position, null, null);
	}

	public TeleportTarget {
		Objects.requireNonNull(world, "world");
		Objects.requireNonNull(position, "position");
		if ((yRot == null) != (xRot == null)) {
			throw new IllegalArgumentException("yRot and xRot must be both present or both absent");
		}
	}

	public static TeleportTarget of(ServerLevel world, Vec3 position) {
		return new TeleportTarget(world, position);
	}

	public static TeleportTarget of(ServerLevel world, Vec3 position, float yRot, float xRot) {
		return new TeleportTarget(world, position, yRot, xRot);
	}

	public static TeleportTarget centered(ServerLevel world, BlockPos pos) {
		Objects.requireNonNull(pos, "pos");
		return new TeleportTarget(world, new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
	}

	public TeleportTarget withPosition(Vec3 position) {
		return new TeleportTarget(world, position, yRot, xRot);
	}

	public float effectiveYRot(ServerPlayer player) {
		return yRot == null ? player.getYRot() : yRot;
	}

	public float effectiveXRot(ServerPlayer player) {
		return xRot == null ? player.getXRot() : xRot;
	}

	public BlockPos blockPos() {
		return BlockPos.containing(position);
	}

	public ResourceKey<Level> dimension() {
		return world.dimension();
	}
}
