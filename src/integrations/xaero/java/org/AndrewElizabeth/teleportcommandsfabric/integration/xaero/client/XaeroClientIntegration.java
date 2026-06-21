package org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.client;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.client.MapWaypointAdapterRegistry;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public final class XaeroClientIntegration implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FabricLoader loader = FabricLoader.getInstance();
		if (loader.isModLoaded("xaerominimap") || loader.isModLoaded("xaeroworldmap")) {
			MapWaypointAdapterRegistry.register(new XaeroMapWaypointAdapter());
		}
	}
}
