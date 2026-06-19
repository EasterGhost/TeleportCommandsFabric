package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin;

import java.util.Locale;

public record AdminHelpRequest(AdminHelpTopic topic, String module, String language, AdminRuntimeInfo runtimeInfo) {
	public AdminHelpRequest {
		topic = topic == null ? AdminHelpTopic.OVERVIEW : topic;
		module = module == null ? "" : module.trim().toLowerCase(Locale.ROOT);
		language = language == null || language.isBlank() ? "en_us" : language.toLowerCase(Locale.ROOT);
		runtimeInfo = runtimeInfo == null ? AdminRuntimeInfo.versionOnly("unknown") : runtimeInfo;
	}

	public static AdminHelpRequest overview(String language, AdminRuntimeInfo runtimeInfo) {
		return new AdminHelpRequest(AdminHelpTopic.OVERVIEW, "", language, runtimeInfo);
	}

	public static AdminHelpRequest admin(String language, AdminRuntimeInfo runtimeInfo) {
		return new AdminHelpRequest(AdminHelpTopic.ADMIN, "", language, runtimeInfo);
	}

	public static AdminHelpRequest config(String language, AdminRuntimeInfo runtimeInfo) {
		return new AdminHelpRequest(AdminHelpTopic.CONFIG, "", language, runtimeInfo);
	}

	public static AdminHelpRequest configModule(String module, String language, AdminRuntimeInfo runtimeInfo) {
		return new AdminHelpRequest(AdminHelpTopic.CONFIG_MODULE, module, language, runtimeInfo);
	}
}
