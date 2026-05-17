package org.AndrewElizabeth.teleportcommandsfabric.config;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;

import java.time.Duration;

public final class ConfigApplier {
	private ConfigApplier() {
	}

	public static void applyStorage() {
		StoragePolicy policy = storagePolicy();

		if (TeleportCommands.PLAYER_PROFILE_MANAGER != null) {
			TeleportCommands.PLAYER_PROFILE_MANAGER.setSaveInterval(policy.saveInterval());
			TeleportCommands.PLAYER_PROFILE_MANAGER.setDeleteInvalidHomes(policy.deleteInvalidHomes());
		}
		if (TeleportCommands.GLOBAL_PROFILE_MANAGER != null) {
			TeleportCommands.GLOBAL_PROFILE_MANAGER.setSaveInterval(policy.saveInterval());
			TeleportCommands.GLOBAL_PROFILE_MANAGER.setDeleteInvalidWarps(policy.deleteInvalidWarps());
		}
	}

	private static StoragePolicy storagePolicy() {
		return ConfigManager.query(config -> new StoragePolicy(
				Duration.ofSeconds(config.getStorage().getAutoSaveIntervalSeconds()),
				config.getHome().isDeleteInvalid(),
				config.getWarp().isDeleteInvalid()));
	}

	private record StoragePolicy(Duration saveInterval, boolean deleteInvalidHomes, boolean deleteInvalidWarps) {
	}
}
