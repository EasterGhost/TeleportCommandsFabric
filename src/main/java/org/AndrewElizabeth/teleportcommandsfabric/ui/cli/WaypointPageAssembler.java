package org.AndrewElizabeth.teleportcommandsfabric.ui.cli;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.cache.WarpListCache;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.PageView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.WaypointPageKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.WaypointPageRequest;

import java.util.List;
import java.util.Objects;

public final class WaypointPageAssembler {
	private final WarpListCache warpListCache;

	public WaypointPageAssembler(WarpListCache warpListCache) {
		this.warpListCache = Objects.requireNonNull(warpListCache, "warpListCache");
	}

	public PageView<NamedLocationView> page(WaypointPageRequest request) {
		List<NamedLocationView> rows = filteredRows(request);
		return Pagination.page(rows, request.query().page());
	}

	public void invalidateWarpCache() {
		warpListCache.invalidateAll();
	}

	private List<NamedLocationView> filteredRows(WaypointPageRequest request) {
		if (request.kind() == WaypointPageKind.WARPS) {
			return warpListCache.rows(request.locations(), request.query());
		}
		return WaypointRows.filterAndSort(request.locations(), request.query());
	}
}
