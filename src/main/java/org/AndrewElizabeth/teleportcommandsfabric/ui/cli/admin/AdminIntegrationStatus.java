package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin;

public record AdminIntegrationStatus(String labelKey, String stateKey) {
	public AdminIntegrationStatus {
		labelKey = labelKey == null || labelKey.isBlank()
				? "commands.teleport_commands.admin.info.integration.unknown"
				: labelKey;
		stateKey = stateKey == null || stateKey.isBlank()
				? "commands.teleport_commands.admin.info.integration.unknown"
				: stateKey;
	}

	public static AdminIntegrationStatus available(String labelKey) {
		return new AdminIntegrationStatus(labelKey,
				"commands.teleport_commands.admin.info.integration.available");
	}

	public static AdminIntegrationStatus loaded(String labelKey) {
		return new AdminIntegrationStatus(labelKey,
				"commands.teleport_commands.admin.info.integration.loaded");
	}
}
