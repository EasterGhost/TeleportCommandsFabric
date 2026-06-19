package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin;

import java.util.List;

public record AdminRuntimeInfo(String version, List<AdminIntegrationStatus> integrations) {
	public AdminRuntimeInfo {
		version = version == null || version.isBlank() ? "unknown" : version;
		integrations = integrations == null ? List.of() : List.copyOf(integrations);
	}

	public static AdminRuntimeInfo of(String version, List<AdminIntegrationStatus> integrations) {
		return new AdminRuntimeInfo(version, integrations);
	}

	public static AdminRuntimeInfo versionOnly(String version) {
		return new AdminRuntimeInfo(version, List.of());
	}
}
