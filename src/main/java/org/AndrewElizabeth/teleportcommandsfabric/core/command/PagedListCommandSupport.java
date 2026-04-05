package org.AndrewElizabeth.teleportcommandsfabric.core.command;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class PagedListCommandSupport {
	@FunctionalInterface
	public interface EmptyHandler {
		void run();
	}

	@FunctionalInterface
	public interface InvalidPageHandler {
		void run(int requestedPage, int totalPages);
	}

	@FunctionalInterface
	public interface PageRenderer<T> {
		MutableComponent render(List<T> pageEntries, int currentPage, int totalPages);
	}

	@FunctionalInterface
	public interface PagePickerRenderer {
		MutableComponent render(int currentPage, int totalPages);
	}

	private PagedListCommandSupport() {
	}

	public static <T> void displayPage(ServerPlayer player, List<T> entries, int page, EmptyHandler onEmpty,
			InvalidPageHandler onInvalidPage, PageRenderer<T> renderer) {
		if (entries.isEmpty()) {
			onEmpty.run();
			return;
		}

		int totalPages = PaginationCommandSupport.getTotalPages(entries.size());
		if (!PaginationCommandSupport.isValidPage(page, totalPages)) {
			onInvalidPage.run(page, totalPages);
			return;
		}

		player.sendSystemMessage(renderer.render(PaginationCommandSupport.getPageEntries(entries, page), page, totalPages),
				false);
	}

	public static <T> void displayPagePicker(ServerPlayer player, List<T> entries, int currentPage, EmptyHandler onEmpty,
			InvalidPageHandler onInvalidPage, PagePickerRenderer renderer) {
		if (entries.isEmpty()) {
			onEmpty.run();
			return;
		}

		int totalPages = PaginationCommandSupport.getTotalPages(entries.size());
		if (!PaginationCommandSupport.isValidPage(currentPage, totalPages)) {
			onInvalidPage.run(currentPage, totalPages);
			return;
		}

		player.sendSystemMessage(renderer.render(currentPage, totalPages), false);
	}
}
