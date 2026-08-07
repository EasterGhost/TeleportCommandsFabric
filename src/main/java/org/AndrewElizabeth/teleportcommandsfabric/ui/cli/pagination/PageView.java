package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.pagination;

import java.util.List;

public record PageView<T>(List<T> entries, int currentPage, int totalPages, int totalItems) {
	public PageView {
		entries = List.copyOf(entries);
		currentPage = Math.max(1, currentPage);
		totalPages = Math.max(1, totalPages);
		totalItems = Math.max(0, totalItems);
	}
}
