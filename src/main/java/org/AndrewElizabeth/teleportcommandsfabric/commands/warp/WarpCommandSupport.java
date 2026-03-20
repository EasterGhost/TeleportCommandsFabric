package org.AndrewElizabeth.teleportcommandsfabric.commands.warp;

import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager.STORAGE;

final class WarpCommandSupport {
	private WarpCommandSupport() {
	}

	static void printWarps(CommandSourceStack source, ServerPlayer player) {
		List<NamedLocation> warps = STORAGE.getWarps();
		if (warps.isEmpty()) {
			WarpMessages.sendHomeless(player);
			return;
		}

		player.displayClientMessage(WarpFormatter.buildWarpList(source, player, warps), false);
	}

	static void printWarps(ServerPlayer player) {
		printWarps(player.createCommandSourceStack(), player);
	}

	static Optional<NamedLocation> resolveWarpForCommand(String warpName, ServerPlayer player, boolean silent) {
		Optional<NamedLocation> optionalWarp = org.AndrewElizabeth.teleportcommandsfabric.services.LocationResolver
				.resolveWarp(warpName);
		if (optionalWarp.isEmpty() && !silent) {
			WarpMessages.sendNotFound(player);
		}
		return optionalWarp;
	}

	static void printAdminWarpMap(ServerPlayer player) {
		List<NamedLocation> warps = STORAGE.getWarps();
		if (warps.isEmpty()) {
			WarpMessages.sendHomeless(player);
			return;
		}

		player.displayClientMessage(WarpAdminFormatter.buildWarpMapList(player, warps), false);
	}
}
