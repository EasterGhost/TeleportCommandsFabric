package org.AndrewElizabeth.teleportcommandsfabric.commands.home;

import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.common.Player;
import org.AndrewElizabeth.teleportcommandsfabric.commands.common.PaginationCommandSupport;

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
		printHomes(player, playerStorage, 1);
	}

	static void printHomes(ServerPlayer player, Player playerStorage, int page) {
		List<NamedLocation> homes = playerStorage.getHomes();
		if (homes.isEmpty()) {
			HomeMessages.sendHomeless(player);
			return;
		}

		int totalPages = PaginationCommandSupport.getTotalPages(homes.size());
		if (!PaginationCommandSupport.isValidPage(page, totalPages)) {
			HomeMessages.sendInvalidPage(player, page, totalPages);
			return;
		}

		player.displayClientMessage(
				HomeFormatter.buildHomeList(
						player,
						playerStorage,
						PaginationCommandSupport.getPageEntries(homes, page),
						page,
						totalPages),
				false);
	}

	static void printHomePagePicker(ServerPlayer player, Player playerStorage, int currentPage) {
		List<NamedLocation> homes = playerStorage.getHomes();
		if (homes.isEmpty()) {
			HomeMessages.sendHomeless(player);
			return;
		}

		int totalPages = PaginationCommandSupport.getTotalPages(homes.size());
		if (!PaginationCommandSupport.isValidPage(currentPage, totalPages)) {
			HomeMessages.sendInvalidPage(player, currentPage, totalPages);
			return;
		}

		player.displayClientMessage(HomeFormatter.buildHomePagePicker(player, currentPage, totalPages), false);
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
