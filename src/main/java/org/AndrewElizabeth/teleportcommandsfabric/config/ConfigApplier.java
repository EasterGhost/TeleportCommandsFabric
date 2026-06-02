package org.AndrewElizabeth.teleportcommandsfabric.config;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportEffects;
import org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.XaeroSyncServer;

import java.time.Duration;

public final class ConfigApplier {
	private ConfigApplier() {
	}

	public static void applyRuntime() {
		RuntimePolicy policy = runtimePolicy();

		if (TeleportCommands.PLAYER_PROFILE_MANAGER != null) {
			TeleportCommands.PLAYER_PROFILE_MANAGER.setSaveInterval(policy.storage().saveInterval());
			TeleportCommands.PLAYER_PROFILE_MANAGER.setDeleteInvalidHomes(policy.storage().deleteInvalidHomes());
		}
		if (TeleportCommands.GLOBAL_PROFILE_MANAGER != null) {
			TeleportCommands.GLOBAL_PROFILE_MANAGER.setSaveInterval(policy.storage().saveInterval());
			TeleportCommands.GLOBAL_PROFILE_MANAGER.setDeleteInvalidWarps(policy.storage().deleteInvalidWarps());
		}
		TeleportEffects.setEnabled(policy.teleportEffects());
		XaeroSyncServer.applyConfig(policy.xaero().enabled(), policy.xaero().syncIntervalSeconds(),
				policy.xaero().persistWaypointSets(), policy.xaero().warpSetName(), policy.xaero().homeSetName());
	}

	private static RuntimePolicy runtimePolicy() {
		return ConfigManager.query(config -> new RuntimePolicy(
				new StoragePolicy(
						Duration.ofSeconds(config.getStorage().getAutoSaveIntervalSeconds()),
						config.getHome().isDeleteInvalid(),
						config.getWarp().isDeleteInvalid()),
				config.getTeleporting().isTeleportEffects(),
				new XaeroPolicy(
						config.getXaero().isEnabled(),
						config.getXaero().getSyncIntervalSeconds(),
						config.getXaero().isPersistWaypointSets(),
						config.getXaero().getWarpSetName(),
						config.getXaero().getHomeSetName())));
	}

	private record RuntimePolicy(StoragePolicy storage, boolean teleportEffects, XaeroPolicy xaero) {
	}

	private record StoragePolicy(Duration saveInterval, boolean deleteInvalidHomes, boolean deleteInvalidWarps) {
	}

	private record XaeroPolicy(boolean enabled, int syncIntervalSeconds, boolean persistWaypointSets,
			String warpSetName, String homeSetName) {
	}
}
