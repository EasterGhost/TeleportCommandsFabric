package org.AndrewElizabeth.teleportcommandsfabric.commands.warp;

import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.commands.common.PaginationCommandSupport;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager.STORAGE;

final class WarpCommandSupport {
	private WarpCommandSupport() {
	}

	static void printWarps(CommandSourceStack source, ServerPlayer player) {
		printWarps(source, player, 1);
	}

	static void printWarps(CommandSourceStack source, ServerPlayer player, int page) {
		List<NamedLocation> warps = STORAGE.getWarps();
		if (warps.isEmpty()) {
			WarpMessages.sendHomeless(player);
			return;
		}

		int totalPages = PaginationCommandSupport.getTotalPages(warps.size());
		if (!PaginationCommandSupport.isValidPage(page, totalPages)) {
			WarpMessages.sendInvalidPage(player, page, totalPages);
			return;
		}

		player.displayClientMessage(
				WarpFormatter.buildWarpList(
						source,
						player,
						PaginationCommandSupport.getPageEntries(warps, page),
						page,
						totalPages),
				false);
	}

	static void printWarps(ServerPlayer player) {
		printWarps(player.createCommandSourceStack(), player, 1);
	}

	static void printWarps(ServerPlayer player, int page) {
		printWarps(player.createCommandSourceStack(), player, page);
	}

	static void printWarpPagePicker(ServerPlayer player, int currentPage) {
		List<NamedLocation> warps = STORAGE.getWarps();
		if (warps.isEmpty()) {
			WarpMessages.sendHomeless(player);
			return;
		}

		int totalPages = PaginationCommandSupport.getTotalPages(warps.size());
		if (!PaginationCommandSupport.isValidPage(currentPage, totalPages)) {
			WarpMessages.sendInvalidPage(player, currentPage, totalPages);
			return;
		}

		player.displayClientMessage(WarpFormatter.buildWarpPagePicker(player, currentPage, totalPages), false);
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

		player.displayClientMessage(WarpAdminFormatter.buildWarpMapList(player, warps), false);
	}
}
