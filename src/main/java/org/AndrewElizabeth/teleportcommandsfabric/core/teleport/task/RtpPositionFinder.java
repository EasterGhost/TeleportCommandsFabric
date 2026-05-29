package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.rtp.RtpTeleportPending;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Optional;
import java.util.Objects;
import java.util.SplittableRandom;

public final class RtpPositionFinder {
	private RtpPositionFinder() {
	}

	public static Optional<BlockPos> findSafeRandomPosition(ServerLevel world, RtpTeleportPending pending,
			int attemptBudget, SplittableRandom random) {
		if (attemptBudget <= 0) {
			return Optional.empty();
		}
		Objects.requireNonNull(random, "random");

		int minY = world.getMinY() + 1;
		int maxY = world.getMaxY();
		int maxR2 = pending.maxRadius() * pending.maxRadius();
		int minR2 = pending.minRadius() * pending.minRadius();
		BlockPos center = pending.center();
		int centerX = center.getX();
		int centerY = center.getY();
		int centerZ = center.getZ();
		boolean restrictNetherRoofBedrock = world.dimension().equals(Level.NETHER) && centerY < 128;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos headPos = new BlockPos.MutableBlockPos();

		for (int attempt = 0; attempt < attemptBudget; attempt++) {
			int dx = random.nextInt(pending.maxRadius() * 2 + 1) - pending.maxRadius();
			int dz = random.nextInt(pending.maxRadius() * 2 + 1) - pending.maxRadius();
			int horizontalR2 = dx * dx + dz * dz;
			if (horizontalR2 > maxR2) {
				continue;
			}

			int x = centerX + dx;
			int z = centerZ + dz;
			int yMin = Math.max(minY, centerY - pending.maxRadius() + 1);
			int yMax = Math.min(maxY, centerY + pending.maxRadius());
			yMax = Math.min(yMax, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
			if (yMin > yMax) {
				continue;
			}

			int dy = random.nextInt(yMin, yMax + 1) - centerY;
			int distance2 = horizontalR2 + dy * dy;
			if (distance2 > maxR2 || distance2 < minR2) {
				continue;
			}

			pos.set(x, centerY + dy, z);
			if (isSafeTeleportPos(world, pos, belowPos, headPos, restrictNetherRoofBedrock)) {
				return Optional.of(pos.immutable());
			}
		}

		return Optional.empty();
	}

	private static boolean isSafeTeleportPos(ServerLevel world, BlockPos pos, BlockPos.MutableBlockPos belowPos,
			BlockPos.MutableBlockPos headPos, boolean restrictNetherRoofBedrock) {
		belowPos.set(pos.getX(), pos.getY() - 1, pos.getZ());
		BlockState belowState = world.getBlockState(belowPos);
		if (restrictNetherRoofBedrock && belowPos.getY() == 127 && belowState.is(Blocks.BEDROCK)) {
			return false;
		}
		if (belowState.isAir() || !belowState.getFluidState().isEmpty()) {
			return false;
		}
		if (belowState.getCollisionShape(world, belowPos).isEmpty()) {
			return false;
		}

		BlockState feetState = world.getBlockState(pos);
		headPos.set(pos.getX(), pos.getY() + 1, pos.getZ());
		BlockState headState = world.getBlockState(headPos);
		return feetState.isAir() && headState.isAir();
	}
}
