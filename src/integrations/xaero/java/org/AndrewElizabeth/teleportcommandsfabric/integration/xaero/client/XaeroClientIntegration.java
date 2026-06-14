package org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class XaeroClientIntegration implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		XaeroSyncClient.initialize();
	}
}
