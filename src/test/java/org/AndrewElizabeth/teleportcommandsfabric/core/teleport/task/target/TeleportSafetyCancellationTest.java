package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

class TeleportSafetyCancellationTest {
	@Test
	void cancellationBeforeSearchAvoidsWorldReads() {
		AtomicInteger reads = new AtomicInteger();

		assertThrows(CancellationException.class, () -> TeleportSafety.getSafeBlockPos(
				BlockPos.ZERO,
				null,
				pos -> {
					reads.incrementAndGet();
					return null;
				},
				() -> true));

		assertEquals(0, reads.get());
	}

	@Test
	void cancellationAfterBlockReadStopsBeforeCollisionLookup() {
		AtomicBoolean cancelled = new AtomicBoolean();
		AtomicInteger reads = new AtomicInteger();

		assertThrows(CancellationException.class, () -> TeleportSafety.getSafeBlockPos(
				BlockPos.ZERO,
				null,
				pos -> {
					reads.incrementAndGet();
					cancelled.set(true);
					return null;
				},
				cancelled::get));

		assertEquals(1, reads.get());
	}
}
