package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Set;

final class TeleportSafetyRules {
	private static final Set<Block> UNSAFE_COLLISION_FREE_BLOCKS = Set.of(
			Blocks.LAVA,
			Blocks.END_PORTAL,
			Blocks.END_GATEWAY,
			Blocks.FIRE,
			Blocks.SOUL_FIRE,
			Blocks.WITHER_ROSE,
			Blocks.POWDER_SNOW,
			Blocks.NETHER_PORTAL);

	private TeleportSafetyRules() {
	}

	static boolean isUnsafeCollisionFreeBlock(Block block) {
		return UNSAFE_COLLISION_FREE_BLOCKS.contains(block);
	}
}
