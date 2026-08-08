package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.target;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

final class LoadedChunkBlockStateReader implements TeleportSafety.BlockStateReader {
	private static final BlockState MISSING_BLOCK_STATE = Blocks.LAVA.defaultBlockState();

	private final long[] chunkKeys;
	private final LevelChunk[] chunks;
	private final boolean complete;

	private LoadedChunkBlockStateReader(long[] chunkKeys, LevelChunk[] chunks, boolean complete) {
		this.chunkKeys = chunkKeys;
		this.chunks = chunks;
		this.complete = complete;
	}

	static LoadedChunkBlockStateReader create(ServerLevel world, BlockPos basePos) {
		int minChunkX = (basePos.getX() - TeleportSafety.SEARCH_RADIUS) >> 4;
		int maxChunkX = (basePos.getX() + TeleportSafety.SEARCH_RADIUS) >> 4;
		int minChunkZ = (basePos.getZ() - TeleportSafety.SEARCH_RADIUS) >> 4;
		int maxChunkZ = (basePos.getZ() + TeleportSafety.SEARCH_RADIUS) >> 4;
		int chunkCount = (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
		long[] chunkKeys = new long[chunkCount];
		LevelChunk[] chunks = new LevelChunk[chunkCount];
		boolean complete = true;
		int index = 0;
		for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
			for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
				LevelChunk chunk = world.getChunkSource().getChunkNow(chunkX, chunkZ);
				if (chunk == null) {
					complete = false;
				}
				chunkKeys[index] = ChunkPos.asLong(chunkX, chunkZ);
				chunks[index] = chunk;
				index++;
			}
		}
		return new LoadedChunkBlockStateReader(chunkKeys, chunks, complete);
	}

	boolean complete() {
		return complete;
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		long chunkKey = ChunkPos.asLong(pos);
		for (int i = 0; i < chunkKeys.length; i++) {
			if (chunkKeys[i] == chunkKey) {
				LevelChunk chunk = chunks[i];
				return chunk == null ? MISSING_BLOCK_STATE : chunk.getBlockState(pos);
			}
		}
		return MISSING_BLOCK_STATE;
	}
}
