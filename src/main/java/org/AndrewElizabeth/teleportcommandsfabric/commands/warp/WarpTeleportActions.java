package org.AndrewElizabeth.teleportcommandsfabric.commands.warp;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.services.NamedLocationTeleportService;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager.STORAGE;

final class WarpTeleportActions {
	private WarpTeleportActions() {
	}

	static void goToWarp(ServerPlayer player, String warpName) throws Exception {
		Optional<NamedLocation> optionalWarp = WarpCommandSupport.resolveWarpForCommand(warpName, player, false);
		if (optionalWarp.isEmpty()) {
			return;
		}

		goToWarp(player, optionalWarp.get());
	}

	private static void goToWarp(ServerPlayer player, NamedLocation warp) throws Exception {
		Optional<ServerLevel> optionalWorld = NamedLocationTeleportService.resolveWorld(warp);

		if (optionalWorld.isEmpty()) {
			Constants.LOGGER.warn(
					"({}) Error while going to the warp \"{}\"! \nCouldn't find a world with the id: \"{}\"",
					player.getName().getString(),
					warp.getName(),
					warp.getWorldString());

			WarpMessages.sendWorldNotFound(player);

			if (org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG.getWarp().isDeleteInvalid()) {
				STORAGE.removeWarp(warp);
				Constants.LOGGER.info("Deleted invalid warp '{}'", warp.getName());
				WarpMessages.sendDeletedInvalid(player);
			}
			return;
		}

		ServerLevel warpWorld = optionalWorld.get();
		if (NamedLocationTeleportService.isAlreadyAtDestination(player, warpWorld, warp)) {
			WarpMessages.send(player, "commands.teleport_commands.warp.goSame", ChatFormatting.AQUA);
		} else if (NamedLocationTeleportService.teleportToNamedLocation(player, warpWorld, warp)) {
			WarpMessages.send(player, "commands.teleport_commands.warp.go");
		}
	}
}
