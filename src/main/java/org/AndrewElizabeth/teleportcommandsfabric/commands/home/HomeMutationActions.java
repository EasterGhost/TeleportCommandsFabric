package org.AndrewElizabeth.teleportcommandsfabric.commands.home;

import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.common.Player;
import org.AndrewElizabeth.teleportcommandsfabric.services.LocationResolver;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager.STORAGE;

final class HomeMutationActions {
	private HomeMutationActions() {
	}

	static void setHome(ServerPlayer player, String homeName) throws Exception {
		homeName = LocationResolver.normalizeName(homeName);
		String worldString = WorldResolver.getDimensionId(player.level().dimension());

		Player playerStorage = STORAGE.addPlayer(player.getStringUUID());

		int maxHomes = org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager.CONFIG.home.getPlayerMaximum();
		boolean homeExists = playerStorage.getHome(homeName).isPresent();
		if (!homeExists && maxHomes > 0 && playerStorage.getHomes().size() >= maxHomes) {
			HomeMessages.sendMaxReached(player, maxHomes);
			return;
		}

		NamedLocation home = NamedLocation.create(
				homeName,
				player.getBlockX(),
				player.getY(),
				player.getBlockZ(),
				worldString);

		boolean homeAlreadyExists = playerStorage.addHome(home);

		if (homeAlreadyExists) {
			HomeMessages.send(player, "commands.teleport_commands.home.exists", ChatFormatting.RED);
		} else {
			if (playerStorage.getHomes().size() == 1) {
				playerStorage.setDefaultHomeByUuid(home.getUuid());
			}

			HomeMessages.send(player, "commands.teleport_commands.home.set");
		}
	}

	static void deleteHome(ServerPlayer player, String homeName) throws Exception {
		HomeCommandSupport.withPlayerStorage(player, playerStorage -> {
			Optional<NamedLocation> optionalHome = HomeCommandSupport.resolveHomeForCommand(
					playerStorage,
					homeName,
					player,
					ChatFormatting.RED);
			if (optionalHome.isEmpty()) {
				return;
			}

			playerStorage.deleteHome(optionalHome.get());
			HomeMessages.send(player, "commands.teleport_commands.home.delete");
		});
	}

	static void renameHome(ServerPlayer player, String homeName, String newHomeName) throws Exception {
		final String resolvedNewHomeName = LocationResolver.normalizeName(newHomeName);

		HomeCommandSupport.withPlayerStorage(player, playerStorage -> {
			if (LocationResolver.resolveHome(playerStorage, resolvedNewHomeName).isPresent()) {
				HomeMessages.sendNameExists(player);
				return;
			}

			Optional<NamedLocation> optionalHome = HomeCommandSupport.resolveHomeForCommand(
					playerStorage,
					homeName,
					player,
					ChatFormatting.RED);
			if (optionalHome.isEmpty()) {
				return;
			}

			UUID homeUuid = optionalHome.get().getUuid();
			optionalHome.get().setName(resolvedNewHomeName);
			if (homeUuid.equals(playerStorage.getDefaultHomeUuid())) {
				playerStorage.setDefaultHomeByUuid(homeUuid);
			}

			HomeMessages.send(player, "commands.teleport_commands.home.rename");
		});
	}

	static void setDefaultHome(ServerPlayer player, String homeName) throws Exception {
		HomeCommandSupport.withPlayerStorage(player, playerStorage -> {
			Optional<NamedLocation> optionalHome = HomeCommandSupport.resolveHomeForCommand(
					playerStorage,
					homeName,
					player,
					ChatFormatting.RED);
			if (optionalHome.isEmpty()) {
				return;
			}

			if (optionalHome.get().getUuid().equals(playerStorage.getDefaultHomeUuid())) {
				HomeMessages.send(player, "commands.teleport_commands.home.defaultSame", ChatFormatting.AQUA);
				return;
			}

			playerStorage.setDefaultHomeByUuid(optionalHome.get().getUuid());
			HomeMessages.send(player, "commands.teleport_commands.home.default");
		});
	}

	static void updateHome(ServerPlayer player, String homeName) throws Exception {
		String worldString = WorldResolver.getDimensionId(player.level().dimension());

		HomeCommandSupport.withPlayerStorage(player, playerStorage -> {
			Optional<NamedLocation> optionalHome = HomeCommandSupport.resolveHomeForCommand(
					playerStorage,
					homeName,
					player,
					ChatFormatting.RED);
			if (optionalHome.isEmpty()) {
				return;
			}

			if (isSameLocation(player, optionalHome.get(), worldString)) {
				HomeMessages.send(player, "commands.teleport_commands.home.updateSame", ChatFormatting.AQUA);
				return;
			}

			optionalHome.get().setCoordinates(
					player.getBlockX(),
					player.getY(),
					player.getBlockZ(),
					worldString);
			HomeMessages.send(player, "commands.teleport_commands.home.update");
		});
	}

	private static boolean isSameLocation(ServerPlayer player, NamedLocation location, String worldString) {
		return player.blockPosition().equals(location.getBlockPos())
				&& Objects.equals(worldString, location.getWorldString());
	}

}
