package org.AndrewElizabeth.teleportcommandsfabric.modules.admin;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminIntegrationStatus;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminRuntimeInfo;

import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;

final class AdminRuntimeInfoProvider {
	private static final String XAERO_INTEGRATION_MOD_ID = "teleport_commands_fabric_xaero";
	private static final String JOURNEYMAP_INTEGRATION_MOD_ID = "teleport_commands_fabric_journeymap";
	private static final String BLUEMAP_INTEGRATION_MOD_ID = "teleport_commands_fabric_bluemap";

	private AdminRuntimeInfoProvider() {
	}

	static AdminRuntimeInfo current() {
		FabricLoader loader = FabricLoader.getInstance();
		return AdminRuntimeInfo.of(ModConstants.VERSION, integrations(loader));
	}

	private static List<AdminIntegrationStatus> integrations(FabricLoader loader) {
		List<AdminIntegrationStatus> integrations = new ArrayList<>();
		if (loader.isModLoaded(XAERO_INTEGRATION_MOD_ID)) {
			integrations.add(new AdminIntegrationStatus("commands.teleport_commands.admin.info.integration.xaero"));
		}
		if (loader.isModLoaded(JOURNEYMAP_INTEGRATION_MOD_ID)) {
			integrations.add(new AdminIntegrationStatus("commands.teleport_commands.admin.info.integration.journeymap"));
		}
		if (loader.isModLoaded(BLUEMAP_INTEGRATION_MOD_ID)) {
			integrations.add(new AdminIntegrationStatus("commands.teleport_commands.admin.info.integration.bluemap"));
		}
		return List.copyOf(integrations);
	}
}
