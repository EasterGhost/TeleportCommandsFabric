package org.AndrewElizabeth.teleportcommandsfabric.services;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.Set;

public final class TeleportSafety {
	private static final Set<Block> UNSAFE_COLLISION_FREE_BLOCKS = Set.of(
			Blocks.LAVA, Blocks.END_PORTAL, Blocks.END_GATEWAY,
			Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.POWDER_SNOW,
			Blocks.NETHER_PORTAL);

	private TeleportSafety() {
	}

	public static Optional<BlockPos> getSafeBlockPos(BlockPos blockPos, ServerLevel world) {
		int row = 1;
		int rows = 3;

		int blockPosX = blockPos.getX();
		int blockPosY = blockPos.getY();
		int blockPosZ = blockPos.getZ();

		if (isBlockPosSafe(blockPos, world)) {
			return Optional.of(blockPos);
		}

		while (row <= rows) {
			for (int z = -row; z <= row; z++) {
				for (int x = -row; x <= row; x++) {
					for (int y = -row; y <= row; y++) {
						if ((x == -row || x == row) || (z == -row || z == row) || (y == -row || y == row)) {
							BlockPos newPos = new BlockPos(blockPosX + x, blockPosY + y, blockPosZ + z);

							if (isBlockPosSafe(newPos, world)) {
								return Optional.of(newPos);
							}
						}
					}
				}
			}

			row++;
		}

		return Optional.empty();
	}

	private static boolean isBlockPosSafe(BlockPos bottomPlayer, ServerLevel world) {
		BlockPos belowPlayer = new BlockPos(bottomPlayer.getX(), bottomPlayer.getY() - 1, bottomPlayer.getZ());
		BlockState belowState = world.getBlockState(belowPlayer);

		BlockState bottomState = world.getBlockState(bottomPlayer);

		BlockPos topPlayer = new BlockPos(bottomPlayer.getX(), bottomPlayer.getY() + 1, bottomPlayer.getZ());
		BlockState topState = world.getBlockState(topPlayer);

		if ((belowState.is(Blocks.WATER)
				|| !belowState.getCollisionShape(world, belowPlayer).isEmpty())
				&& (bottomState.getCollisionShape(world, bottomPlayer).isEmpty()
						&& !UNSAFE_COLLISION_FREE_BLOCKS.contains(bottomState.getBlock()))
				&& (topState.getCollisionShape(world, topPlayer).isEmpty()
						&& !UNSAFE_COLLISION_FREE_BLOCKS.contains(topState.getBlock()))) {
			return true;
		}
		return false;
	}
}
