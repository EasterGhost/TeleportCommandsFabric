package org.AndrewElizabeth.teleportcommandsfabric.modules.admin;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminIntegrationStatus;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminRuntimeInfo;

import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;

final class AdminRuntimeInfoProvider {
	private static final String CLIENT_MAP_SYNC_MOD_ID = "teleport_commands_fabric_integration_common";
	private static final String BLUEMAP_INTEGRATION_MOD_ID = "teleport_commands_fabric_bluemap";
	private static final String BLUEMAP_MOD_ID = "bluemap";

	private AdminRuntimeInfoProvider() {
	}

	static AdminRuntimeInfo current() {
		FabricLoader loader = FabricLoader.getInstance();
		return AdminRuntimeInfo.of(ModConstants.VERSION, integrations(loader));
	}

	private static List<AdminIntegrationStatus> integrations(FabricLoader loader) {
		List<AdminIntegrationStatus> integrations = new ArrayList<>();
		if (loader.isModLoaded(CLIENT_MAP_SYNC_MOD_ID)) {
			integrations.add(AdminIntegrationStatus.available(
					"commands.teleport_commands.admin.info.integration.client_sync"));
		}
		if (loader.isModLoaded(BLUEMAP_INTEGRATION_MOD_ID) && loader.isModLoaded(BLUEMAP_MOD_ID)) {
			integrations.add(AdminIntegrationStatus.loaded(
					"commands.teleport_commands.admin.info.integration.bluemap"));
		}
		return List.copyOf(integrations);
	}
}
