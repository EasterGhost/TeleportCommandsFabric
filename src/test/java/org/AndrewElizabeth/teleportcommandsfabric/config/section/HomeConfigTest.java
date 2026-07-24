package org.AndrewElizabeth.teleportcommandsfabric.config.section;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeConfigTest {
	@Test
	void usesConservativeSharedHomeDefaults() {
		HomeConfig config = new HomeConfig();

		assertEquals(1, config.getSharedHomeMaximum());
		assertEquals(60, config.getSharedHomeBroadcastCooldownSeconds());
	}

	@Test
	void clampsSharedHomeLimitsToSupportedMinimums() {
		HomeConfig config = new HomeConfig();

		config.setSharedHomeMaximum(0);
		config.setSharedHomeBroadcastCooldownSeconds(1);

		assertEquals(1, config.getSharedHomeMaximum());
		assertEquals(10, config.getSharedHomeBroadcastCooldownSeconds());
	}
}
