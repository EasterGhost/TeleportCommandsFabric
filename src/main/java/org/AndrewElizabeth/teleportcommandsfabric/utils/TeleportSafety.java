package org.AndrewElizabeth.teleportcommandsfabric.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;
import java.util.Set;

public final class TeleportSafety {
	private static final Set<String> UNSAFE_COLLISION_FREE_BLOCKS = Set.of("block.minecraft.lava",
			"block.minecraft.flowing_lava", "block.minecraft.end_portal", "block.minecraft.end_gateway",
			"block.minecraft.fire", "block.minecraft.soul_fire", "block.minecraft.powder_snow",
			"block.minecraft.nether_portal");

	private TeleportSafety() {
	}

	// checks a 7x7x7 location around the player in order to find a safe place to
	// teleport them to.
	public static Optional<BlockPos> getSafeBlockPos(BlockPos blockPos, ServerLevel world) {
		int row = 1;
		int rows = 3;

		int blockPosX = blockPos.getX();
		int blockPosY = blockPos.getY();
		int blockPosZ = blockPos.getZ();

		if (isBlockPosSafe(blockPos, world)) {
			return Optional.of(blockPos); // safe location found!

		} else {
			// find a safe location in an x row radius
			while (row <= rows) {
				for (int z = -row; z <= row; z++) {
					for (int x = -row; x <= row; x++) {
						for (int y = -row; y <= row; y++) {

							// checks if we are on the outer layer of the row, not on the inside
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

			return Optional.empty(); // no safe location found!
		}
	}

	// checks if a BlockPos is safe, used by the teleportSafetyChecker.
	private static boolean isBlockPosSafe(BlockPos bottomPlayer, ServerLevel world) {
		BlockPos belowPlayer = new BlockPos(bottomPlayer.getX(), bottomPlayer.getY() - 1, bottomPlayer.getZ());
		String belowPlayerId = world.getBlockState(belowPlayer).getBlock().getDescriptionId();

		String bottomPlayerId = world.getBlockState(bottomPlayer).getBlock().getDescriptionId();

		BlockPos topPlayer = new BlockPos(bottomPlayer.getX(), bottomPlayer.getY() + 1, bottomPlayer.getZ());
		String topPlayerId = world.getBlockState(topPlayer).getBlock().getDescriptionId();

		if ((belowPlayerId.equals("block.minecraft.water")
				|| !world.getBlockState(belowPlayer).getCollisionShape(world, belowPlayer).isEmpty())
				&& (world.getBlockState(bottomPlayer).getCollisionShape(world, bottomPlayer).isEmpty()
						&& !UNSAFE_COLLISION_FREE_BLOCKS.contains(bottomPlayerId))
				&& (world.getBlockState(topPlayer).getCollisionShape(world, topPlayer).isEmpty()
						&& !UNSAFE_COLLISION_FREE_BLOCKS.contains(topPlayerId))) {
			return true;
		}
		return false;
	}
}
