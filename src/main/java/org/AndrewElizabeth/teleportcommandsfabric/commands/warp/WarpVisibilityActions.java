package org.AndrewElizabeth.teleportcommandsfabric.commands.warp;

import org.AndrewElizabeth.teleportcommandsfabric.commands.common.VisibilityCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.common.Player;

import net.minecraft.server.level.ServerPlayer;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager.STORAGE;

final class WarpVisibilityActions {
	private WarpVisibilityActions() {
	}

	static void setPlayerVisibility(ServerPlayer player, String warpName, boolean visible) throws Exception {
		withPlayerData(player, playerData -> VisibilityCommandSupport.update(
				visible,
				() -> WarpCommandSupport.resolveWarpForCommand(warpName, player, false),
				warp -> !playerData.isWarpHidden(warp.getUuid()),
				(warp, desiredVisible) -> updatePlayerWarpVisibility(playerData, warp, desiredVisible),
				() -> WarpMessages.sendPlayerMapVisibilityAlready(player, visible),
				() -> {
					WarpMessages.sendPlayerMapVisibilityChanged(player, visible);
					WarpCommandSupport.printWarps(player, 1);
				}));
	}

	static void setPlayerVisibilitySilently(ServerPlayer player, String warpName, boolean visible) throws Exception {
		withPlayerData(player, playerData -> VisibilityCommandSupport.update(
				visible,
				() -> WarpCommandSupport.resolveWarpForCommand(warpName, player, true),
				warp -> !playerData.isWarpHidden(warp.getUuid()),
				(warp, desiredVisible) -> updatePlayerWarpVisibility(playerData, warp, desiredVisible),
				() -> {
				},
				() -> {
				}));
	}

	static void setPlayerVisibilitySilentlyAndShowPage(ServerPlayer player, String warpName, boolean visible, int page)
			throws Exception {
		withPlayerData(player, playerData -> VisibilityCommandSupport.update(
				visible,
				() -> WarpCommandSupport.resolveWarpForCommand(warpName, player, true),
				warp -> !playerData.isWarpHidden(warp.getUuid()),
				(warp, desiredVisible) -> updatePlayerWarpVisibility(playerData, warp, desiredVisible),
				() -> WarpCommandSupport.printWarps(player, page),
				() -> WarpCommandSupport.printWarps(player, page)));
	}

	static void setGlobalVisibility(ServerPlayer player, String warpName, boolean visible) throws Exception {
		VisibilityCommandSupport.update(
				visible,
				() -> WarpCommandSupport.resolveWarpForCommand(warpName, player, false),
				NamedLocation::isXaeroVisible,
				NamedLocation::setXaeroVisible,
				() -> WarpMessages.sendMapVisibilityAlready(player, visible),
				() -> {
					WarpMessages.sendMapVisibilityChanged(player, visible);
					WarpCommandSupport.printAdminWarpMap(player);
				});
	}

	private static void updatePlayerWarpVisibility(Player playerData, NamedLocation warp, boolean visible)
			throws Exception {
		if (visible) {
			playerData.showWarp(warp.getUuid());
		} else {
			playerData.hideWarp(warp.getUuid());
		}
	}

	private static void withPlayerData(ServerPlayer player, WarpPlayerAction action) throws Exception {
		action.run(STORAGE.addPlayer(player.getStringUUID()));
	}

	@FunctionalInterface
	private interface WarpPlayerAction {
		void run(Player playerData) throws Exception;
	}
}
