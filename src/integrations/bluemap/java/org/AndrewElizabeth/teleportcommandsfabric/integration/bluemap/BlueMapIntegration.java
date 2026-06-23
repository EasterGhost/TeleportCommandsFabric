package org.AndrewElizabeth.teleportcommandsfabric.integration.bluemap;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.AndrewElizabeth.teleportcommandsfabric.utils.DebugLog;

public final class BlueMapIntegration implements ModInitializer {
	private static final String BLUEMAP_MOD_ID = "bluemap";

	@Override
	public void onInitialize() {
		if (!FabricLoader.getInstance().isModLoaded(BLUEMAP_MOD_ID)) {
			DebugLog.info("BlueMap integration is bundled but BlueMap is not loaded.");
			return;
		}
		BlueMapWarpMarkerSync.initialize();
	}
}
