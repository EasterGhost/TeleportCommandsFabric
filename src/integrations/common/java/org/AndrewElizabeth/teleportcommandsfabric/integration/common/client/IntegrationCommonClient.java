package org.AndrewElizabeth.teleportcommandsfabric.integration.common.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class IntegrationCommonClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MapWaypointSyncClient.initialize();
	}
}
