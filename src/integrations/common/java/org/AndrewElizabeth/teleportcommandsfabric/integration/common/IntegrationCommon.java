package org.AndrewElizabeth.teleportcommandsfabric.integration.common;

import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.config.RuntimeConfigHooks;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.server.MapWaypointSyncServer;

import net.fabricmc.api.ModInitializer;

public final class IntegrationCommon implements ModInitializer {
	@Override
	public void onInitialize() {
		RuntimeConfigHooks.register(IntegrationCommon::applyRuntimeConfig);
		MapWaypointSyncServer.initialize();
	}

	private static void applyRuntimeConfig() {
		ConfigManager.query(config -> {
			MapWaypointSyncServer.applyConfig(
					config.getIntegration().isEnabled(),
					config.getIntegration().getSyncIntervalSeconds(),
					config.getIntegration().isPersistWaypointSets(),
					config.getIntegration().getWarpSetName(),
					config.getIntegration().getHomeSetName());
			return null;
		});
	}
}
