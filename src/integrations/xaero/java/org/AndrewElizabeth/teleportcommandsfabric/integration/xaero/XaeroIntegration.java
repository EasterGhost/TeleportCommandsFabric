package org.AndrewElizabeth.teleportcommandsfabric.integration.xaero;

import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.config.RuntimeConfigHooks;

import net.fabricmc.api.ModInitializer;

public final class XaeroIntegration implements ModInitializer {
	@Override
	public void onInitialize() {
		RuntimeConfigHooks.register(XaeroIntegration::applyRuntimeConfig);
		XaeroSyncServer.initialize();
	}

	private static void applyRuntimeConfig() {
		ConfigManager.query(config -> {
			XaeroSyncServer.applyConfig(
					config.getXaero().isEnabled(),
					config.getXaero().getSyncIntervalSeconds(),
					config.getXaero().isPersistWaypointSets(),
					config.getXaero().getWarpSetName(),
					config.getXaero().getHomeSetName());
			return null;
		});
	}
}
