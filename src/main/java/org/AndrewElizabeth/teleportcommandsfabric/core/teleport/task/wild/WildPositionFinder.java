package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.SurfacePositionSafety;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.wild.WildTeleportPending;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

final class WildPositionFinder {
	static final int CHUNKS_PER_BATCH = 4;
	private static final int CANDIDATES_PER_CHUNK = 8;
	private static final int MAX_ANCHOR_SAMPLE_ATTEMPTS = 4096;
	private static final double PLAYER_HALF_WIDTH = 0.3D;

	private WildPositionFinder() {
	}

	static List<ChunkPos> sampleChunkBatch(ServerLevel world, WildTeleportPending pending,
			Set<Long> excludedChunks) {
		List<ChunkPos> chunks = new ArrayList<>(CHUNKS_PER_BATCH);
		Set<Long> selected = new HashSet<>();
		BlockPos center = pending.center();
		SplittableRandom random = pending.random();

		for (int attempt = 0; attempt < MAX_ANCHOR_SAMPLE_ATTEMPTS && chunks.size() < CHUNKS_PER_BATCH; attempt++) {
			ChunkPos chunkPos = sampleChunk(center, pending.minRadius(), pending.maxRadius(), random);
			if (chunkPos == null) {
				continue;
			}

			long packed = chunkPos.pack();
			if (excludedChunks.contains(packed) || !selected.add(packed)) {
				continue;
			}
			if (!world.getWorldBorder().isWithinBounds(chunkPos)) {
				continue;
			}

			chunks.add(chunkPos);
		}

		return List.copyOf(chunks);
	}

	static List<Candidate> findSafePositions(ServerLevel world, WildTeleportPending pending,
			Map<ChunkPos, LevelChunk> chunks) {
		List<Candidate> candidates = new ArrayList<>(chunks.size() * CANDIDATES_PER_CHUNK);
		SplittableRandom random = pending.random();
		BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos headPos = new BlockPos.MutableBlockPos();

		for (Map.Entry<ChunkPos, LevelChunk> entry : chunks.entrySet()) {
			LevelChunk chunk = entry.getValue();
			if (chunk == null || !chunk.hasPrimedHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES)) {
				continue;
			}

			Set<Integer> columns = new HashSet<>(CANDIDATES_PER_CHUNK);
			while (columns.size() < CANDIDATES_PER_CHUNK) {
				columns.add(random.nextInt(256));
			}

			for (int column : columns) {
				int localX = column & 15;
				int localZ = column >>> 4;
				int x = entry.getKey().getBlockX(localX);
				int z = entry.getKey().getBlockZ(localZ);
				int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, localX, localZ) + 1;
				BlockPos feetPos = new BlockPos(x, y, z);
				if (!isCandidateWithinBounds(world, feetPos)
						|| !isSafePosition(chunk, feetPos, belowPos, headPos)) {
					continue;
				}
				candidates.add(new Candidate(feetPos, entry.getKey()));
			}
		}

		return List.copyOf(candidates);
	}

	static ChunkPos sampleChunk(BlockPos center, int minRadius, int maxRadius, SplittableRandom random) {
		double minRadiusSquared = (double) minRadius * minRadius;
		double maxRadiusSquared = (double) maxRadius * maxRadius;
		double radiusSquared = minRadius == maxRadius
				? minRadiusSquared
				: minRadiusSquared + random.nextDouble() * (maxRadiusSquared - minRadiusSquared);
		double radius = Math.sqrt(radiusSquared);
		double angle = random.nextDouble(Math.PI * 2.0D);
		long anchorX = center.getX() + Math.round(Math.cos(angle) * radius);
		long anchorZ = center.getZ() + Math.round(Math.sin(angle) * radius);
		long chunkX = Math.floorDiv(anchorX, 16L);
		long chunkZ = Math.floorDiv(anchorZ, 16L);
		if (chunkX < -ChunkPos.MAX_COORDINATE_VALUE || chunkX > ChunkPos.MAX_COORDINATE_VALUE
				|| chunkZ < -ChunkPos.MAX_COORDINATE_VALUE || chunkZ > ChunkPos.MAX_COORDINATE_VALUE) {
			return null;
		}
		return new ChunkPos((int) chunkX, (int) chunkZ);
	}

	private static boolean isCandidateWithinBounds(ServerLevel world, BlockPos feetPos) {
		return world.isInsideBuildHeight(feetPos.getY() - 1)
				&& world.isInsideBuildHeight(feetPos.getY() + 1)
				&& world.getWorldBorder().isWithinBounds(feetPos.getX() + 0.5D, feetPos.getZ() + 0.5D,
						PLAYER_HALF_WIDTH);
	}

	private static boolean isSafePosition(LevelChunk chunk, BlockPos feetPos, BlockPos.MutableBlockPos belowPos,
			BlockPos.MutableBlockPos headPos) {
		belowPos.set(feetPos.getX(), feetPos.getY() - 1, feetPos.getZ());
		BlockState belowState = chunk.getBlockState(belowPos);
		if (!SurfacePositionSafety.isSafeSupport(chunk, belowPos, belowState)) {
			return false;
		}

		BlockState feetState = chunk.getBlockState(feetPos);
		headPos.set(feetPos.getX(), feetPos.getY() + 1, feetPos.getZ());
		BlockState headState = chunk.getBlockState(headPos);
		return SurfacePositionSafety.isBodyClear(chunk, feetPos, feetState)
				&& SurfacePositionSafety.isBodyClear(chunk, headPos, headState);
	}

	record Candidate(BlockPos position, ChunkPos chunkPos) {
		Candidate {
			position = position.immutable();
		}
	}
}
