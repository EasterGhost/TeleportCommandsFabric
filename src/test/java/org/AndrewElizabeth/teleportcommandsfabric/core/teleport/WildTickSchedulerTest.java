package org.AndrewElizabeth.teleportcommandsfabric.core.teleport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WildTickSchedulerTest {
	@Test
	void drainsDueKeysInTickAndInsertionOrder() {
		WildTickScheduler<String> scheduler = new WildTickScheduler<>();
		scheduler.schedule("late", 10L);
		scheduler.schedule("first", 5L);
		scheduler.schedule("second", 5L);

		assertEquals(List.of(), scheduler.drainDue(4L));
		assertEquals(List.of("first", "second"), scheduler.drainDue(5L));
		assertEquals(List.of("late"), scheduler.drainDue(10L));
	}

	@Test
	void rescheduleAndCancelRemoveOldEntries() {
		WildTickScheduler<String> scheduler = new WildTickScheduler<>();
		scheduler.schedule("rescheduled", 10L);
		scheduler.schedule("rescheduled", 3L);
		scheduler.schedule("cancelled", 3L);
		scheduler.cancel("cancelled");

		assertEquals(List.of("rescheduled"), scheduler.drainDue(3L));
		assertEquals(List.of(), scheduler.drainDue(10L));
	}
}
