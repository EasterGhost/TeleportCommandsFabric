package org.AndrewElizabeth.teleportcommandsfabric.commands.home;

import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.common.Player;
import org.AndrewElizabeth.teleportcommandsfabric.commands.common.DimensionFilterCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.commands.common.PagedListCommandSupport;

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
		printHomes(player, playerStorage, 1, null);
	}

	static void printHomes(ServerPlayer player, Player playerStorage, int page) {
		printHomes(player, playerStorage, page, null);
	}

	static void printHomes(ServerPlayer player, Player playerStorage, int page, String dimensionFilter) {
		String normalizedDimensionFilter = DimensionFilterCommandSupport.normalizeDimensionFilter(dimensionFilter);
		List<NamedLocation> homes = DimensionFilterCommandSupport.sortAndFilter(
				playerStorage.getHomes(),
				normalizedDimensionFilter);
		PagedListCommandSupport.displayPage(
				player,
				homes,
				page,
				() -> sendEmptyMessage(player, normalizedDimensionFilter),
				(requestedPage, totalPages) -> HomeMessages.sendInvalidPage(player, requestedPage, totalPages),
				(pageEntries, currentPage, totalPages) -> HomeFormatter.buildHomeList(
						player,
						playerStorage,
						pageEntries,
						currentPage,
						totalPages,
						normalizedDimensionFilter));
	}

	static void printHomePagePicker(ServerPlayer player, Player playerStorage, int currentPage) {
		printHomePagePicker(player, playerStorage, currentPage, null);
	}

	static void printHomePagePicker(ServerPlayer player, Player playerStorage, int currentPage, String dimensionFilter) {
		String normalizedDimensionFilter = DimensionFilterCommandSupport.normalizeDimensionFilter(dimensionFilter);
		List<NamedLocation> homes = DimensionFilterCommandSupport.sortAndFilter(
				playerStorage.getHomes(),
				normalizedDimensionFilter);
		PagedListCommandSupport.displayPagePicker(
				player,
				homes,
				currentPage,
				() -> sendEmptyMessage(player, normalizedDimensionFilter),
				(requestedPage, totalPages) -> HomeMessages.sendInvalidPage(player, requestedPage, totalPages),
				(page, totalPages) -> HomeFormatter.buildHomePagePicker(
						player,
						page,
						totalPages,
						normalizedDimensionFilter));
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

	private static void sendEmptyMessage(ServerPlayer player, String dimensionFilter) {
		if (dimensionFilter == null) {
			HomeMessages.sendHomeless(player);
			return;
		}

		HomeMessages.sendNoHomesInDimension(player, dimensionFilter);
	}
}
