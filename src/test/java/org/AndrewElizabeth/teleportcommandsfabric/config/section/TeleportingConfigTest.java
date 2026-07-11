package org.AndrewElizabeth.teleportcommandsfabric.config.section;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TeleportingConfigTest {
	@Test
	void clampsPreloadRadiusToSupportedRange() {
		TeleportingConfig config = new TeleportingConfig();

		config.setPreloadRadiusChunks(-1);
		assertEquals(TeleportingConfig.MIN_PRELOAD_RADIUS_CHUNKS, config.getPreloadRadiusChunks());

		config.setPreloadRadiusChunks(2);
		assertEquals(2, config.getPreloadRadiusChunks());

		config.setPreloadRadiusChunks(4);
		assertEquals(TeleportingConfig.MAX_PRELOAD_RADIUS_CHUNKS, config.getPreloadRadiusChunks());
	}
}
