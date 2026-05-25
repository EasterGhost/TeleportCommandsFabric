package org.AndrewElizabeth.teleportcommandsfabric.ui.cli;

import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.PageView;

import java.util.List;

public final class Pagination {
	public static final int DEFAULT_PAGE_SIZE = 4;

	private Pagination() {
	}

	public static int totalPages(int totalItems) {
		return totalPages(totalItems, DEFAULT_PAGE_SIZE);
	}

	public static int totalPages(int totalItems, int pageSize) {
		int safeTotal = Math.max(0, totalItems);
		int safePageSize = Math.max(1, pageSize);
		return Math.max(1, (safeTotal + safePageSize - 1) / safePageSize);
	}

	public static <T> PageView<T> page(List<T> entries, int requestedPage) {
		return page(entries, requestedPage, DEFAULT_PAGE_SIZE);
	}

	public static <T> PageView<T> page(List<T> entries, int requestedPage, int pageSize) {
		List<T> safeEntries = List.copyOf(entries);
		int totalPages = totalPages(safeEntries.size(), pageSize);
		int currentPage = Math.min(Math.max(1, requestedPage), totalPages);
		int safePageSize = Math.max(1, pageSize);
		int fromIndex = (currentPage - 1) * safePageSize;
		int toIndex = Math.min(fromIndex + safePageSize, safeEntries.size());
		return new PageView<>(safeEntries.subList(fromIndex, toIndex), currentPage, totalPages, safeEntries.size());
	}
}
