package org.AndrewElizabeth.teleportcommandsfabric.core.teleport;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.wild.WildRequest;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.wild.WildTeleportPending;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WildServiceTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void firstBatchStartsTwoTicksBeforeDelayExpires() {
		assertEquals(108L, WildService.firstBatchSubmissionTick(pending(100L, 10)));
		assertEquals(100L, WildService.firstBatchSubmissionTick(pending(100L, 2)));
		assertEquals(100L, WildService.firstBatchSubmissionTick(pending(100L, 1)));
		assertEquals(100L, WildService.firstBatchSubmissionTick(pending(100L, 0)));
	}

	private static WildTeleportPending pending(long createTick, int delayTicks) {
		return new WildTeleportPending(UUID.randomUUID(), 1L, createTick,
				new WildRequest(512, 4096, delayTicks, 0L, true), BlockPos.ZERO, Level.OVERWORLD);
	}
}
