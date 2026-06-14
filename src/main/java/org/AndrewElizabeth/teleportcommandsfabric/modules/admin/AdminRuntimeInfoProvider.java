package org.AndrewElizabeth.teleportcommandsfabric.modules.admin;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminIntegrationStatus;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminRuntimeInfo;

import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

final class AdminRuntimeInfoProvider {
	private static final String XAERO_INTEGRATION_MOD_ID = "teleport_commands_fabric_xaero";

	private AdminRuntimeInfoProvider() {
	}

	static AdminRuntimeInfo current() {
		FabricLoader loader = FabricLoader.getInstance();
		return AdminRuntimeInfo.of(ModConstants.VERSION, integrations(loader));
	}

	private static List<AdminIntegrationStatus> integrations(FabricLoader loader) {
		if (!loader.isModLoaded(XAERO_INTEGRATION_MOD_ID)) {
			return List.of();
		}
		return List.of(new AdminIntegrationStatus("commands.teleport_commands.admin.info.integration.xaero"));
	}
}
