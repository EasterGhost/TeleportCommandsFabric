package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model;

public record AdminModuleStatus(String moduleKey, String labelKey, boolean enabled) {
	public AdminModuleStatus {
		moduleKey = moduleKey == null ? "" : moduleKey.trim();
		labelKey = labelKey == null ? "" : labelKey.trim();
	}
}
