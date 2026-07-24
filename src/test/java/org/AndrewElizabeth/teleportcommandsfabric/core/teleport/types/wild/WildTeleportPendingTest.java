package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.wild;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildTeleportPendingTest {
	@Test
	void requestRejectsInvalidRangesAndTiming() {
		assertThrows(IllegalArgumentException.class, () -> new WildRequest(127, 4096, 0, 0L, true));
		assertThrows(IllegalArgumentException.class, () -> new WildRequest(512, 511, 0, 0L, true));
		assertThrows(IllegalArgumentException.class, () -> new WildRequest(512, 4096, -1, 0L, true));
		assertThrows(IllegalArgumentException.class, () -> new WildRequest(512, 4096, 0, -1L, true));
	}

	@Test
	void pendingFreezesCenterAndUsesOperationDelay() {
		BlockPos.MutableBlockPos sourceCenter = new BlockPos.MutableBlockPos(10, 70, -20);
		WildRequest request = new WildRequest(512, 4096, 4, 3000L, true);
		WildTeleportPending pending = new WildTeleportPending(UUID.randomUUID(), 7L, 100L, request,
				sourceCenter, Level.OVERWORLD);

		sourceCenter.set(100, 80, 100);

		assertEquals(new BlockPos(10, 70, -20), pending.center());
		assertEquals(Level.OVERWORLD, pending.dimension());
		assertEquals(512, pending.minRadius());
		assertEquals(4096, pending.maxRadius());
		assertEquals(104L, pending.delayUntilTick());
		assertFalse(pending.isDelayDone(103L));
		assertTrue(pending.isDelayDone(104L));
	}
}
