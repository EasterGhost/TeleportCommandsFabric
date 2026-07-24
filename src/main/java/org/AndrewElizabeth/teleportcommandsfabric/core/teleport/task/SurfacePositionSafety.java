package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public final class SurfacePositionSafety {
	private SurfacePositionSafety() {
	}

	public static boolean isSafeSupport(BlockGetter reader, BlockPos pos, BlockState state) {
		return !state.isAir()
				&& state.getFluidState().isEmpty()
				&& !state.getCollisionShape(reader, pos).isEmpty();
	}

	public static boolean isBodyClear(BlockGetter reader, BlockPos pos, BlockState state) {
		if (state.isAir()) {
			return true;
		}
		if (!state.getFluidState().isEmpty() || !state.getCollisionShape(reader, pos).isEmpty()) {
			return false;
		}
		return !SafetyBlockRules.isUnsafeCollisionFreeBlock(state.getBlock());
	}
}
