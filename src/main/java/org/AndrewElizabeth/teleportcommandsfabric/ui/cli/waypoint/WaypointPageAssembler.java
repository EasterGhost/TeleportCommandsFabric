package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.cache.WarpListCache;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.pagination.PageView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.pagination.Pagination;

import java.util.List;
import java.util.Objects;

final class WaypointPageAssembler {
	private final WarpListCache warpListCache;

	WaypointPageAssembler(WarpListCache warpListCache) {
		this.warpListCache = Objects.requireNonNull(warpListCache, "warpListCache");
	}

	PageView<NamedLocationView> page(WaypointPageRequest request) {
		List<NamedLocationView> rows = filteredRows(request);
		return Pagination.page(rows, request.query().page());
	}

	void invalidateWarpCache() {
		warpListCache.invalidateAll();
	}

	List<NamedLocationView> filteredRows(WaypointPageRequest request) {
		Objects.requireNonNull(request, "request");
		if (request.kind() == WaypointPageKind.WARPS) {
			return warpListCache.rows(request.locations(), request.query());
		}
		return WaypointRows.filterAndSort(request.locations(), request.query());
	}
}
