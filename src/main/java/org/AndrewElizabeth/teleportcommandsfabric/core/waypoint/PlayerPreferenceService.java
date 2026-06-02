package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerPreferenceService {
	private PlayerPreferenceService() {
	}

	public static CompletableFuture<Void> setWarpVisibility(UUID playerUuid, UUID warpUuid, boolean visible, PlayerProfileManager profileManager) {
		return profileManager.mutateIfChanged(playerUuid, profile -> {
			boolean isHidden = profile.isWarpHidden(warpUuid);
			if (visible == !isHidden) {
				return false;
			}
			if (visible) {
				profile.showWarp(warpUuid);
			} else {
				profile.hideWarp(warpUuid);
			}
			return true;
		}).thenApply(ignored -> null);
	}
}
