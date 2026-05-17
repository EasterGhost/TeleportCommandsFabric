package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerPreferenceService {
	private PlayerPreferenceService() {
	}

	public static CompletableFuture<Void> setWarpVisibility(UUID playerUuid, UUID warpUuid, boolean visible, PlayerProfileManager profileManager) {
		return profileManager.query(playerUuid, profile -> profile.isWarpHidden(warpUuid))
				.thenCompose(isHidden -> {
					if (visible == !isHidden) {
						return CompletableFuture.completedFuture(null);
					}
					return profileManager.mutateVoid(playerUuid, profile -> {
						if (visible) {
							profile.showWarp(warpUuid);
						} else {
							profile.hideWarp(warpUuid);
						}
					});
				});
	}
}
