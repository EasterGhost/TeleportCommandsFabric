package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin;

import java.util.Locale;

public record AdminHelpRequest(AdminHelpTopic topic, String module, String language, String version) {
	public AdminHelpRequest {
		topic = topic == null ? AdminHelpTopic.OVERVIEW : topic;
		module = module == null ? "" : module.trim().toLowerCase(Locale.ROOT);
		language = language == null || language.isBlank() ? "en_us" : language.toLowerCase(Locale.ROOT);
		version = version == null || version.isBlank() ? "unknown" : version;
	}

	public static AdminHelpRequest overview(String language, String version) {
		return new AdminHelpRequest(AdminHelpTopic.OVERVIEW, "", language, version);
	}

	public static AdminHelpRequest admin(String language, String version) {
		return new AdminHelpRequest(AdminHelpTopic.ADMIN, "", language, version);
	}

	public static AdminHelpRequest config(String language, String version) {
		return new AdminHelpRequest(AdminHelpTopic.CONFIG, "", language, version);
	}

	public static AdminHelpRequest configModule(String module, String language, String version) {
		return new AdminHelpRequest(AdminHelpTopic.CONFIG_MODULE, module, language, version);
	}
}
