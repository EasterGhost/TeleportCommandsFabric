package org.AndrewElizabeth.teleportcommandsfabric.config;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportEffects;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportRotation;
import org.AndrewElizabeth.teleportcommandsfabric.utils.DebugLog;

import java.time.Duration;

public final class ConfigApplier {
	private ConfigApplier() {
	}

	public static void applyRuntime() {
		RuntimePolicy policy = runtimePolicy();

		DebugLog.setEnabled(policy.debugEnabled());
		if (TeleportCommands.PLAYER_PROFILE_MANAGER != null) {
			TeleportCommands.PLAYER_PROFILE_MANAGER.setSaveInterval(policy.storage().saveInterval());
		}
		if (TeleportCommands.GLOBAL_PROFILE_MANAGER != null) {
			TeleportCommands.GLOBAL_PROFILE_MANAGER.setSaveInterval(policy.storage().saveInterval());
		}
		TeleportEffects.setEnabled(policy.teleportEffects());
		TeleportRotation.setRestoreEnabled(policy.restoreRotation());
		if (TeleportCommands.TELEPORT_SERVICE != null) {
			TeleportCommands.TELEPORT_SERVICE.configurePreload(policy.preload().enabled(), policy.preload().radiusChunks());
		}
		RuntimeConfigHooks.applyAll();
	}

	private static RuntimePolicy runtimePolicy() {
		return ConfigManager.query(config -> new RuntimePolicy(
				config.isDebugEnabled(),
				new StoragePolicy(
						Duration.ofSeconds(config.getStorage().getAutoSaveIntervalSeconds())),
				config.getTeleporting().isTeleportEffects(),
				config.getTeleporting().isRestoreRotation(),
				new PreloadPolicy(
						config.getTeleporting().isPreloadEnabled(),
						config.getTeleporting().getPreloadRadiusChunks())));
	}

	private record RuntimePolicy(boolean debugEnabled, StoragePolicy storage, boolean teleportEffects, boolean restoreRotation, PreloadPolicy preload) {
	}

	private record StoragePolicy(Duration saveInterval) {
	}

	private record PreloadPolicy(boolean enabled, int radiusChunks) {
	}
}
