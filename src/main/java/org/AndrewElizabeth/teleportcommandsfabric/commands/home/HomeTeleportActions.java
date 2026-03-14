package org.AndrewElizabeth.teleportcommandsfabric.commands.home;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.common.Player;
import org.AndrewElizabeth.teleportcommandsfabric.services.NamedLocationTeleportService;
import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

final class HomeTeleportActions {
	private HomeTeleportActions() {
	}

	static void goHome(ServerPlayer player, String homeName) throws Exception {
		HomeCommandSupport.withPlayerStorage(player, playerStorage -> {
			String resolvedHomeName = homeName;

			if (resolvedHomeName.isEmpty()) {
				String defaultHome = playerStorage.getDefaultHome();

				if (defaultHome.isEmpty()) {
					HomeMessages.send(player, "commands.teleport_commands.home.defaultNone", ChatFormatting.AQUA);
					return;
				}
				resolvedHomeName = defaultHome;
			}

			Optional<NamedLocation> optionalHome = HomeCommandSupport.resolveHomeForCommand(
					playerStorage,
					resolvedHomeName,
					player,
					ChatFormatting.AQUA);
			if (optionalHome.isEmpty()) {
				return;
			}

			goHome(player, playerStorage, optionalHome.get());
		});
	}

	private static void goHome(ServerPlayer player, Player playerStorage, NamedLocation home) throws Exception {
		Optional<ServerLevel> optionalWorld = NamedLocationTeleportService.resolveWorld(home);

		if (optionalWorld.isEmpty()) {
			Constants.LOGGER.warn(
					"({}) Error while going to the home \"{}\"! \nCouldn't find a world with the id: \"{}\"",
					player.getName().getString(),
					home.getName(),
					home.getWorldString());

			HomeMessages.sendWorldNotFound(player);

			if (org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG.getHome().isDeleteInvalid()) {
				playerStorage.deleteHomeNoSave(home);
				StorageManager.StorageSaver();
				Constants.LOGGER.info("Deleted invalid home '{}' for player {}", home.getName(),
						player.getName().getString());
				HomeMessages.sendDeletedInvalid(player);
			}
			return;
		}

		ServerLevel homeWorld = optionalWorld.get();
		if (NamedLocationTeleportService.isAlreadyAtDestination(player, homeWorld, home)) {
			HomeMessages.send(player, "commands.teleport_commands.home.goSame", ChatFormatting.AQUA);
		} else if (NamedLocationTeleportService.teleportToNamedLocation(player, homeWorld, home)) {
			HomeMessages.send(player, "commands.teleport_commands.home.go");
		}
	}

}
