package org.AndrewElizabeth.teleportcommandsfabric.commands.home;

import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.common.Player;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager.STORAGE;

final class HomeCommandSupport {
	@FunctionalInterface
	interface HomePlayerAction {
		void run(Player playerStorage) throws Exception;
	}

	private HomeCommandSupport() {
	}

	static void withPlayerStorage(ServerPlayer player, HomePlayerAction action) throws Exception {
		Optional<Player> optionalPlayerStorage = STORAGE.getPlayer(player.getStringUUID());
		if (optionalPlayerStorage.isEmpty()) {
			HomeMessages.sendHomeless(player);
			return;
		}
		action.run(optionalPlayerStorage.get());
	}

	static void withOptionalPlayerStorage(ServerPlayer player, HomePlayerAction action) throws Exception {
		Optional<Player> optionalPlayerStorage = STORAGE.getPlayer(player.getStringUUID());
		if (optionalPlayerStorage.isEmpty()) {
			action.run(null);
			return;
		}
		action.run(optionalPlayerStorage.get());
	}

	static void printHomes(ServerPlayer player, Player playerStorage) {
		List<NamedLocation> homes = playerStorage.getHomes();
		if (homes.isEmpty()) {
			HomeMessages.sendHomeless(player);
			return;
		}

		player.displayClientMessage(HomeFormatter.buildHomeList(player, playerStorage, homes), false);
	}

	static Optional<NamedLocation> resolveHomeForCommand(Player playerStorage, String homeName, ServerPlayer player,
			ChatFormatting notFoundColor) {
		return resolveHomeForCommand(playerStorage, homeName, player, notFoundColor, false);
	}

	static Optional<NamedLocation> resolveHomeForCommand(Player playerStorage, String homeName, ServerPlayer player,
			ChatFormatting notFoundColor, boolean silent) {
		Optional<NamedLocation> optionalHome = org.AndrewElizabeth.teleportcommandsfabric.services.LocationResolver
				.resolveHome(playerStorage, homeName);
		if (optionalHome.isEmpty() && !silent) {
			HomeMessages.sendNotFound(player, notFoundColor);
		}
		return optionalHome;
	}
}
