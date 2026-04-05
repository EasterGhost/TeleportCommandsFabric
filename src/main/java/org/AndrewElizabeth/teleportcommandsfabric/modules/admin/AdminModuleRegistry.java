package org.AndrewElizabeth.teleportcommandsfabric.modules.admin;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG;

final class AdminModuleRegistry {

	record ModuleToggle(Consumer<Boolean> setter, BooleanSupplier enabled, String labelKey) {
	}

	private static final Map<String, ModuleToggle> MODULES = new LinkedHashMap<>();

	static {
		MODULES.put("back", new ModuleToggle(value -> CONFIG.getBack().setEnabled(value),
				() -> CONFIG.getBack().isEnabled(), "commands.teleport_commands.admin.module.back"));
		MODULES.put("home", new ModuleToggle(value -> CONFIG.getHome().setEnabled(value),
				() -> CONFIG.getHome().isEnabled(), "commands.teleport_commands.admin.module.home"));
		MODULES.put("tpa", new ModuleToggle(value -> CONFIG.getTpa().setEnabled(value),
				() -> CONFIG.getTpa().isEnabled(), "commands.teleport_commands.admin.module.tpa"));
		MODULES.put("warp", new ModuleToggle(value -> CONFIG.getWarp().setEnabled(value),
				() -> CONFIG.getWarp().isEnabled(), "commands.teleport_commands.admin.module.warp"));
		MODULES.put("worldspawn", new ModuleToggle(value -> CONFIG.getWorldSpawn().setEnabled(value),
				() -> CONFIG.getWorldSpawn().isEnabled(), "commands.teleport_commands.admin.module.worldspawn"));
		MODULES.put("rtp", new ModuleToggle(value -> CONFIG.getRtp().setEnabled(value),
				() -> CONFIG.getRtp().isEnabled(), "commands.teleport_commands.admin.module.rtp"));
		MODULES.put("xaero", new ModuleToggle(value -> CONFIG.getXaero().setEnabled(value),
				() -> CONFIG.getXaero().isEnabled(), "commands.teleport_commands.admin.module.xaero"));
	}

	private AdminModuleRegistry() {
	}

	static ModuleToggle get(String moduleName) {
		return MODULES.get(moduleName);
	}

	static List<String> allNames() {
		return List.copyOf(MODULES.keySet());
	}

	static List<Map.Entry<String, ModuleToggle>> entries() {
		return List.copyOf(MODULES.entrySet());
	}

	static List<String> enabledNames() {
		return MODULES.entrySet().stream().filter(entry -> entry.getValue().enabled().getAsBoolean())
				.map(Map.Entry::getKey).toList();
	}

	static List<String> disabledNames() {
		return MODULES.entrySet().stream().filter(entry -> !entry.getValue().enabled().getAsBoolean())
				.map(Map.Entry::getKey).toList();
	}
}

