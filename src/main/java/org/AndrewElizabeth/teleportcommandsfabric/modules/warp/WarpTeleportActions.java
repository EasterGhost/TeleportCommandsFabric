package org.AndrewElizabeth.teleportcommandsfabric.modules.warp;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.NamedLocationTeleportService;
import org.AndrewElizabeth.teleportcommandsfabric.models.NamedLocation;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG;

final class WarpTeleportActions {
	private WarpTeleportActions() {
	}

	static void goToWarp(ServerPlayer player, String warpName) throws Exception {
		GlobalWarpSource source = new GlobalWarpSource();
		Optional<NamedLocation> optionalWarp = source.getByName(warpName);
		if (optionalWarp.isEmpty()) {
			WarpMessages.sendNotFound(player);
			return;
		}

		goToWarp(player, source, optionalWarp.get());
	}

	private static void goToWarp(ServerPlayer player, GlobalWarpSource source, NamedLocation warp) throws Exception {
		Optional<ServerLevel> optionalWorld = NamedLocationTeleportService.resolveWorld(warp);

		if (optionalWorld.isEmpty()) {
			ModConstants.LOGGER.warn(
					"({}) Error while going to the warp \"{}\"! \nCouldn't find a world with the id: \"{}\"",
					player.getName().getString(), warp.getName(), warp.getWorldString());

			WarpMessages.sendWorldNotFound(player);

			if (CONFIG.getWarp().isDeleteInvalid()) {
				source.remove(warp);
				ModConstants.LOGGER.info("Deleted invalid warp '{}'", warp.getName());
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

