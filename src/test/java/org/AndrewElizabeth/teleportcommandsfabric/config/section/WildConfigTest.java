package org.AndrewElizabeth.teleportcommandsfabric.config.section;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.wild.WildRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildConfigTest {
	@Test
	void defaultsSupportLongRangeSurfaceSearch() {
		WildConfig config = new WildConfig();

		assertTrue(config.isEnabled());
		assertEquals(512, config.getMinRadius());
		assertEquals(4096, config.getMaxRadius());
	}

	@Test
	void radiusSettersPreserveValidRange() {
		WildConfig config = new WildConfig();

		config.setMinRadius(1);
		assertEquals(WildRequest.MIN_RADIUS, config.getMinRadius());

		config.setMaxRadius(64);
		assertEquals(WildRequest.MIN_RADIUS, config.getMaxRadius());
		assertEquals(WildRequest.MIN_RADIUS, config.getMinRadius());

		config.setMaxRadius(Integer.MAX_VALUE);
		assertEquals(WildRequest.MAX_RADIUS, config.getMaxRadius());
	}

	@Test
	void loweringMaximumAlsoLowersMinimum() {
		WildConfig config = new WildConfig();
		config.setMaxRadius(2048);
		config.setMinRadius(1024);

		config.setMaxRadius(512);

		assertEquals(512, config.getMinRadius());
		assertEquals(512, config.getMaxRadius());
	}
}
