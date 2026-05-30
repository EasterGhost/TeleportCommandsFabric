package org.AndrewElizabeth.teleportcommandsfabric.modules.admin;

import org.AndrewElizabeth.teleportcommandsfabric.config.Config;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminModuleStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

final class AdminModuleRegistry {
	private static final Map<String, ModuleToggle> MODULES = new LinkedHashMap<>();

	static {
		MODULES.put("back", new ModuleToggle(config -> config.getBack().isEnabled(),
				(config, enabled) -> config.getBack().setEnabled(enabled),
				"commands.teleport_commands.admin.module.back"));
		MODULES.put("home", new ModuleToggle(config -> config.getHome().isEnabled(),
				(config, enabled) -> config.getHome().setEnabled(enabled),
				"commands.teleport_commands.admin.module.home"));
		MODULES.put("tpa", new ModuleToggle(config -> config.getTpa().isEnabled(),
				(config, enabled) -> config.getTpa().setEnabled(enabled),
				"commands.teleport_commands.admin.module.tpa"));
		MODULES.put("warp", new ModuleToggle(config -> config.getWarp().isEnabled(),
				(config, enabled) -> config.getWarp().setEnabled(enabled),
				"commands.teleport_commands.admin.module.warp"));
		MODULES.put("worldspawn", new ModuleToggle(config -> config.getWorldSpawn().isEnabled(),
				(config, enabled) -> config.getWorldSpawn().setEnabled(enabled),
				"commands.teleport_commands.admin.module.worldspawn"));
		MODULES.put("rtp", new ModuleToggle(config -> config.getRtp().isEnabled(),
				(config, enabled) -> config.getRtp().setEnabled(enabled),
				"commands.teleport_commands.admin.module.rtp"));
		MODULES.put("xaero", new ModuleToggle(config -> config.getXaero().isEnabled(),
				(config, enabled) -> config.getXaero().setEnabled(enabled),
				"commands.teleport_commands.admin.module.xaero"));
	}

	private AdminModuleRegistry() {
	}

	static ModuleToggle get(String moduleName) {
		return MODULES.get(moduleName);
	}

	static List<String> enabledNames() {
		return ConfigManager.query(config -> MODULES.entrySet().stream()
				.filter(entry -> entry.getValue().enabled(config))
				.map(Map.Entry::getKey)
				.toList());
	}

	static List<String> disabledNames() {
		return ConfigManager.query(config -> MODULES.entrySet().stream()
				.filter(entry -> !entry.getValue().enabled(config))
				.map(Map.Entry::getKey)
				.toList());
	}

	static List<AdminModuleStatus> statuses() {
		return ConfigManager.query(config -> MODULES.entrySet().stream()
				.map(entry -> new AdminModuleStatus(entry.getKey(), entry.getValue().labelKey(),
						entry.getValue().enabled(config)))
				.toList());
	}

	static void setEnabled(String moduleName, boolean enabled) {
		ModuleToggle toggle = get(moduleName);
		if (toggle == null) {
			throw new IllegalArgumentException("Unknown admin module: " + moduleName);
		}
		ConfigManager.mutate(config -> toggle.setEnabled(config, enabled));
	}

	record ModuleToggle(Function<Config, Boolean> enabled, BiConsumer<Config, Boolean> setter, String labelKey) {
		boolean enabled(Config config) {
			return enabled.apply(config);
		}

		void setEnabled(Config config, boolean value) {
			setter.accept(config, value);
		}
	}
}
