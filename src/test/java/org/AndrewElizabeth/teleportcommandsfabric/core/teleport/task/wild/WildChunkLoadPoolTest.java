package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild;

import net.minecraft.server.level.ChunkResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildChunkLoadPoolTest {
	@Test
	void loadRequiresSuccessfulChunkResultAndNoException() {
		assertTrue(WildChunkLoadPool.isSuccessfulLoad(ChunkResult.of("loaded"), null));
		assertFalse(WildChunkLoadPool.isSuccessfulLoad(ChunkResult.error("failed"), null));
		assertFalse(WildChunkLoadPool.isSuccessfulLoad(null, null));
		assertFalse(WildChunkLoadPool.isSuccessfulLoad(ChunkResult.of("loaded"), new RuntimeException()));
	}
}
