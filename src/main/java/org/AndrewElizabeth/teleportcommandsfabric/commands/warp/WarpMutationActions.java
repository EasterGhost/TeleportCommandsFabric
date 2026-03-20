package org.AndrewElizabeth.teleportcommandsfabric.commands.warp;

import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.services.LocationResolver;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager.STORAGE;

final class WarpMutationActions {
	private WarpMutationActions() {
	}

	static void setWarp(ServerPlayer player, String warpName) throws Exception {
		warpName = LocationResolver.normalizeName(warpName);
		String worldString = WorldResolver.getDimensionId(player.level().dimension());

		NamedLocation warp = NamedLocation.create(
				warpName,
				player.getBlockX(),
				player.getY(),
				player.getBlockZ(),
				worldString);

		int maxWarps = org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG.warp.getMaximum();
		boolean warpAlreadyExists = LocationResolver.resolveWarp(warpName).isPresent();
		if (!warpAlreadyExists && maxWarps > 0 && STORAGE.getWarps().size() >= maxWarps) {
			WarpMessages.sendMaxReached(player, maxWarps);
			return;
		}

		boolean warpExists = STORAGE.addWarp(warp);
		if (warpExists) {
			WarpMessages.send(player, "commands.teleport_commands.warp.exists", ChatFormatting.RED);
		} else {
			WarpMessages.send(player, "commands.teleport_commands.warp.set");
		}
	}

	static void deleteWarp(ServerPlayer player, String warpName) throws Exception {
		Optional<NamedLocation> optionalWarp = WarpCommandSupport.resolveWarpForCommand(warpName, player, false);
		if (optionalWarp.isPresent()) {
			STORAGE.removeWarp(optionalWarp.get());
			WarpMessages.send(player, "commands.teleport_commands.warp.delete");
		}
	}

	static void renameWarp(ServerPlayer player, String warpName, String newWarpName) throws Exception {
		newWarpName = LocationResolver.normalizeName(newWarpName);

		if (LocationResolver.resolveWarp(newWarpName).isPresent()) {
			WarpMessages.sendNameExists(player);
			return;
		}

		Optional<NamedLocation> warpToRename = WarpCommandSupport.resolveWarpForCommand(warpName, player, false);
		if (warpToRename.isPresent()) {
			warpToRename.get().setName(newWarpName);
			WarpMessages.send(player, "commands.teleport_commands.warp.rename");
		}
	}
}
