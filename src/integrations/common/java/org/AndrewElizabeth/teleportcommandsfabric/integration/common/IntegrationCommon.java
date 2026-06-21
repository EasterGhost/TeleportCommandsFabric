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
					config.getXaero().isEnabled(),
					config.getXaero().getSyncIntervalSeconds(),
					config.getXaero().isPersistWaypointSets(),
					config.getXaero().getWarpSetName(),
					config.getXaero().getHomeSetName());
			return null;
		});
	}
}
