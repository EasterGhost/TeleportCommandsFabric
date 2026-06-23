package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;

import java.util.OptionalInt;

final class RtpChunkReader implements BlockGetter {
	private static final BlockState MISSING_BLOCK_STATE = Blocks.AIR.defaultBlockState();

	private final int minY;
	private final int height;
	private final int minChunkX;
	private final int minChunkZ;
	private final int chunkXCount;
	private final int chunkZCount;
	private final LevelChunk[] chunks;

	private RtpChunkReader(int minY, int height, int minChunkX, int minChunkZ, int chunkXCount, int chunkZCount,
			LevelChunk[] chunks) {
		this.minY = minY;
		this.height = height;
		this.minChunkX = minChunkX;
		this.minChunkZ = minChunkZ;
		this.chunkXCount = chunkXCount;
		this.chunkZCount = chunkZCount;
		this.chunks = chunks;
	}

	static RtpChunkReader create(ServerLevel world, BlockPos center, int radius) {
		int minChunkX = (center.getX() - radius - 1) >> 4;
		int maxChunkX = (center.getX() + radius + 1) >> 4;
		int minChunkZ = (center.getZ() - radius - 1) >> 4;
		int maxChunkZ = (center.getZ() + radius + 1) >> 4;
		int chunkXCount = maxChunkX - minChunkX + 1;
		int chunkZCount = maxChunkZ - minChunkZ + 1;
		LevelChunk[] chunks = new LevelChunk[chunkXCount * chunkZCount];

		for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
			for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
				chunks[(chunkZ - minChunkZ) * chunkXCount + chunkX - minChunkX] =
						world.getChunkSource().getChunkNow(chunkX, chunkZ);
			}
		}

		return new RtpChunkReader(world.getMinY(), world.getHeight(), minChunkX, minChunkZ, chunkXCount, chunkZCount,
				chunks);
	}

	OptionalInt getSurfaceHeight(Heightmap.Types type, int blockX, int blockZ) {
		LevelChunk chunk = chunkAt(blockX, blockZ);
		if (chunk == null || !chunk.hasPrimedHeightmap(type)) {
			return OptionalInt.empty();
		}
		return OptionalInt.of(chunk.getHeight(type, blockX & 15, blockZ & 15) + 1);
	}

	@Override
	public BlockEntity getBlockEntity(BlockPos pos) {
		return null;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		LevelChunk chunk = chunkAt(pos.getX(), pos.getZ());
		return chunk == null ? MISSING_BLOCK_STATE : chunk.getBlockState(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return getBlockState(pos).getFluidState();
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public int getMinY() {
		return minY;
	}

	private LevelChunk chunkAt(int blockX, int blockZ) {
		int chunkXIndex = (blockX >> 4) - minChunkX;
		int chunkZIndex = (blockZ >> 4) - minChunkZ;
		if (chunkXIndex < 0 || chunkXIndex >= chunkXCount || chunkZIndex < 0 || chunkZIndex >= chunkZCount) {
			return null;
		}
		return chunks[chunkZIndex * chunkXCount + chunkXIndex];
	}
}
