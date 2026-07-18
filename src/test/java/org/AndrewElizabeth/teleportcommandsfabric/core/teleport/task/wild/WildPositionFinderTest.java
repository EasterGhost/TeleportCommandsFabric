package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild;

import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildPositionFinderTest {
	private static final BlockPos CENTER = new BlockPos(0, 64, 0);

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void sampledChunksStayWithinOneChunkOfConfiguredAnnulus() {
		assertSamplesStayWithinChunkTolerance(128, 512);
		assertSamplesStayWithinChunkTolerance(1_000_000, 1_000_000);
	}

	private static void assertSamplesStayWithinChunkTolerance(int minRadius, int maxRadius) {
		SplittableRandom random = new SplittableRandom(42L);
		double tolerance = Math.sqrt(8.0D * 8.0D + 8.0D * 8.0D) + 1.0D;
		for (int sample = 0; sample < 10_000; sample++) {
			ChunkPos chunk = WildPositionFinder.sampleChunk(CENTER, minRadius, maxRadius, random);
			assertNotNull(chunk);
			double dx = chunk.getMiddleBlockX() - CENTER.getX();
			double dz = chunk.getMiddleBlockZ() - CENTER.getZ();
			double distance = Math.sqrt(dx * dx + dz * dz);
			assertTrue(distance >= minRadius - tolerance);
			assertTrue(distance <= maxRadius + tolerance);
		}
	}
}
