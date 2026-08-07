package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildChunkLoadCoordinatorTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void trackedBatchRejectsDuplicatesAndIsReleasedWithOperation() {
		WildChunkLoadCoordinator coordinator = new WildChunkLoadCoordinator();
		UUID playerUuid = UUID.randomUUID();
		List<ChunkPos> chunks = List.of(new ChunkPos(0, 0));

		assertTrue(coordinator.submitBatch(playerUuid, 1L, 0, Level.OVERWORLD, chunks));
		assertFalse(coordinator.submitBatch(playerUuid, 1L, 0, Level.OVERWORLD, chunks));

		coordinator.releaseOperation(playerUuid, 1L);
		assertTrue(coordinator.submitBatch(playerUuid, 1L, 0, Level.OVERWORLD, chunks));
	}
}
