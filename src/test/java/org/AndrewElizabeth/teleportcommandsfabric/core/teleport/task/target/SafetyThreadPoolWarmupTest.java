package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;

class SafetyThreadPoolWarmupTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void syntheticLayoutExercisesSpecialStatesBeforeLateSafePosition() {
		BlockPos basePos = BlockPos.ZERO;
		TeleportSafety.BlockStateReader layout = SafetyThreadPool.createWarmupReader(basePos);
		Set<Block> visitedBlocks = new HashSet<>();

		BlockPos safePos = TeleportSafety.getSafeBlockPos(basePos, null, pos -> {
			BlockState state = layout.getBlockState(pos);
			if (!state.isAir()) {
				visitedBlocks.add(state.getBlock());
			}
			return state;
		}).orElseThrow();

		assertEquals(basePos.offset(3, -3, 3), safePos);
		assertEquals(
				Set.of(Blocks.WATER, Blocks.LAVA, Blocks.WITHER_ROSE, Blocks.OAK_FENCE_GATE, Blocks.STONE),
				visitedBlocks);
		assertTrue(layout.getBlockState(basePos.offset(2, -1, 0)).getValue(FenceGateBlock.OPEN));
	}
}
