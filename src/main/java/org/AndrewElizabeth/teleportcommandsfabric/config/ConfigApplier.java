package org.AndrewElizabeth.teleportcommandsfabric.config;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportEffects;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportRotation;
import org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.XaeroSyncServer;
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
			TeleportCommands.PLAYER_PROFILE_MANAGER.setDeleteInvalidHomes(policy.storage().deleteInvalidHomes());
		}
		if (TeleportCommands.GLOBAL_PROFILE_MANAGER != null) {
			TeleportCommands.GLOBAL_PROFILE_MANAGER.setSaveInterval(policy.storage().saveInterval());
			TeleportCommands.GLOBAL_PROFILE_MANAGER.setDeleteInvalidWarps(policy.storage().deleteInvalidWarps());
		}
		TeleportEffects.setEnabled(policy.teleportEffects());
		TeleportRotation.setRestoreEnabled(policy.restoreRotation());
		if (TeleportCommands.TELEPORT_SERVICE != null) {
			TeleportCommands.TELEPORT_SERVICE.configurePreload(policy.preload().enabled(), policy.preload().radiusChunks());
		}
		XaeroSyncServer.applyConfig(policy.xaero().enabled(), policy.xaero().syncIntervalSeconds(),
				policy.xaero().persistWaypointSets(), policy.xaero().warpSetName(), policy.xaero().homeSetName());
	}

	private static RuntimePolicy runtimePolicy() {
		return ConfigManager.query(config -> new RuntimePolicy(
				config.isDebugEnabled(),
				new StoragePolicy(
						Duration.ofSeconds(config.getStorage().getAutoSaveIntervalSeconds()),
						config.getHome().isDeleteInvalid(),
						config.getWarp().isDeleteInvalid()),
				config.getTeleporting().isTeleportEffects(),
				config.getTeleporting().isRestoreRotation(),
				new PreloadPolicy(
						config.getTeleporting().isPreloadEnabled(),
						config.getTeleporting().getPreloadRadiusChunks()),
				new XaeroPolicy(
						config.getXaero().isEnabled(),
						config.getXaero().getSyncIntervalSeconds(),
						config.getXaero().isPersistWaypointSets(),
						config.getXaero().getWarpSetName(),
						config.getXaero().getHomeSetName())));
	}

	private record RuntimePolicy(boolean debugEnabled, StoragePolicy storage, boolean teleportEffects,
			boolean restoreRotation, PreloadPolicy preload, XaeroPolicy xaero) {
	}

	private record StoragePolicy(Duration saveInterval, boolean deleteInvalidHomes, boolean deleteInvalidWarps) {
	}

	private record PreloadPolicy(boolean enabled, int radiusChunks) {
	}

	private record XaeroPolicy(boolean enabled, int syncIntervalSeconds, boolean persistWaypointSets,
			String warpSetName, String homeSetName) {
	}
}
