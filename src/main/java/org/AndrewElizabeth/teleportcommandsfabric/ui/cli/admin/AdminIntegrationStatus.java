package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin;

public record AdminIntegrationStatus(String labelKey) {
	public AdminIntegrationStatus {
		labelKey = labelKey == null || labelKey.isBlank()
				? "commands.teleport_commands.admin.info.integration.unknown"
				: labelKey;
	}
}
