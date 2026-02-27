package org.AndrewElizabeth.teleportcommandsfabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class TeleportCommandsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		XaeroSyncClient.initialize();
	}
}
