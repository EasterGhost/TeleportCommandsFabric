package org.AndrewElizabeth.teleportcommandsfabric.integration.journeymap;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.integration.common.client.MapWaypointAdapterRegistry;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.CommonEventRegistry;

@JourneyMapPlugin(apiVersion = "2.0.0")
public final class JourneyMapIntegrationPlugin implements IClientPlugin {
	private JourneyMapWaypointAdapter adapter;

	@Override
	public String getModId() {
		return ModConstants.MOD_ID;
	}

	@Override
	public void initialize(IClientAPI api) {
		adapter = new JourneyMapWaypointAdapter(api);
		MapWaypointAdapterRegistry.register(adapter);
		CommonEventRegistry.WAYPOINT_EVENT.subscribe(ModConstants.MOD_ID, adapter::onWaypointEvent);
		ModConstants.LOGGER.info("JourneyMap integration initialized.");
	}
}
