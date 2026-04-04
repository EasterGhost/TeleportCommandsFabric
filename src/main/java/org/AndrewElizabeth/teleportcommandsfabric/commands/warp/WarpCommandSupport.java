package org.AndrewElizabeth.teleportcommandsfabric.commands.warp;

import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.commands.common.DimensionFilterCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.commands.common.PagedListCommandSupport;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager.STORAGE;

final class WarpCommandSupport {
	private WarpCommandSupport() {
	}

	static void printWarps(CommandSourceStack source, ServerPlayer player) {
		printWarps(source, player, 1, null);
	}

	static void printWarps(CommandSourceStack source, ServerPlayer player, int page) {
		printWarps(source, player, page, null);
	}

	static void printWarps(CommandSourceStack source, ServerPlayer player, int page, String dimensionFilter) {
		String normalizedDimensionFilter = DimensionFilterCommandSupport.normalizeDimensionFilter(dimensionFilter);
		List<NamedLocation> warps = DimensionFilterCommandSupport.sortAndFilter(
				STORAGE.getWarps(),
				normalizedDimensionFilter);
		PagedListCommandSupport.displayPage(
				player,
				warps,
				page,
				() -> sendEmptyMessage(player, normalizedDimensionFilter),
				(requestedPage, totalPages) -> WarpMessages.sendInvalidPage(player, requestedPage, totalPages),
				(pageEntries, currentPage, totalPages) -> WarpFormatter.buildWarpList(
						source,
						player,
						pageEntries,
						currentPage,
						totalPages,
						normalizedDimensionFilter));
	}

	static void printWarps(ServerPlayer player, int page) {
		printWarps(player.createCommandSourceStack(), player, page, null);
	}

	static void printWarpPagePicker(ServerPlayer player, int currentPage) {
		printWarpPagePicker(player, currentPage, null);
	}

	static void printWarps(ServerPlayer player, int page, String dimensionFilter) {
		printWarps(player.createCommandSourceStack(), player, page, dimensionFilter);
	}

	static void printWarpPagePicker(ServerPlayer player, int currentPage, String dimensionFilter) {
		String normalizedDimensionFilter = DimensionFilterCommandSupport.normalizeDimensionFilter(dimensionFilter);
		List<NamedLocation> warps = DimensionFilterCommandSupport.sortAndFilter(
				STORAGE.getWarps(),
				normalizedDimensionFilter);
		PagedListCommandSupport.displayPagePicker(
				player,
				warps,
				currentPage,
				() -> sendEmptyMessage(player, normalizedDimensionFilter),
				(requestedPage, totalPages) -> WarpMessages.sendInvalidPage(player, requestedPage, totalPages),
				(page, totalPages) -> WarpFormatter.buildWarpPagePicker(
						player,
						page,
						totalPages,
						normalizedDimensionFilter));
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

		player.sendSystemMessage(WarpAdminFormatter.buildWarpMapList(player, warps), false);
	}

	private static void sendEmptyMessage(ServerPlayer player, String dimensionFilter) {
		if (dimensionFilter == null) {
			WarpMessages.sendHomeless(player);
			return;
		}

		WarpMessages.sendNoWarpsInDimension(player, dimensionFilter);
	}
}
