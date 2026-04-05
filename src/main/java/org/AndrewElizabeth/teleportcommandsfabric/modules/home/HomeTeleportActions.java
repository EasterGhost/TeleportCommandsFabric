package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.NamedLocationTeleportService;
import org.AndrewElizabeth.teleportcommandsfabric.models.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.models.PlayerData;
import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG;

final class HomeTeleportActions {
	private HomeTeleportActions() {
	}

	static void goHome(ServerPlayer player, String homeName) throws Exception {
		PlayerData playerStorage = StorageManager.STORAGE.addPlayer(player.getStringUUID());
		PlayerHomeSource source = new PlayerHomeSource(playerStorage);

		String resolvedHomeName = homeName;

		if (resolvedHomeName.isEmpty()) {
			String defaultHome = playerStorage.getDefaultHome();

			if (defaultHome.isEmpty()) {
				HomeMessages.send(player, "commands.teleport_commands.home.defaultNone", ChatFormatting.AQUA);
				return;
			}
			resolvedHomeName = defaultHome;
		}

		Optional<NamedLocation> optionalHome = source.getByName(resolvedHomeName);
		if (optionalHome.isEmpty()) {
			HomeMessages.sendNotFound(player, ChatFormatting.AQUA);
			return;
		}

		goHome(player, source, optionalHome.get());
	}

	private static void goHome(ServerPlayer player, PlayerHomeSource source, NamedLocation home) throws Exception {
		Optional<ServerLevel> optionalWorld = NamedLocationTeleportService.resolveWorld(home);

		if (optionalWorld.isEmpty()) {
			ModConstants.LOGGER.warn(
					"({}) Error while going to the home \"{}\"! \nCouldn't find a world with the id: \"{}\"",
					player.getName().getString(), home.getName(), home.getWorldString());

			HomeMessages.sendWorldNotFound(player);

			if (CONFIG.getHome().isDeleteInvalid()) {
				source.remove(home);
				ModConstants.LOGGER.info("Deleted invalid home '{}' for player {}", home.getName(), player.getName().getString());
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

