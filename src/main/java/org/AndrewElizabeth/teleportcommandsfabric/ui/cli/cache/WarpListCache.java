package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.cache;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointRows;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WarpListCache {
	private static final int DEFAULT_MAX_CACHED_QUERIES = 128;

	private final int maxCachedQueries;
	private final Map<WaypointListCacheKey, List<NamedLocationView>> cachedRows;
	private Collection<NamedLocationView> sourceIdentity;

	public WarpListCache() {
		this(DEFAULT_MAX_CACHED_QUERIES);
	}

	public WarpListCache(int maxCachedQueries) {
		this.maxCachedQueries = Math.max(1, maxCachedQueries);
		this.cachedRows = new LinkedHashMap<>(16, 0.75F, true);
	}

	public List<NamedLocationView> rows(Collection<NamedLocationView> sourceRows, WaypointListQuery query) {
		WaypointListCacheKey key = WaypointListCacheKey.from(query);
		synchronized (cachedRows) {
			ensureCurrentSource(sourceRows);
			List<NamedLocationView> cached = cachedRows.get(key);
			if (cached != null) {
				return cached;
			}
			List<NamedLocationView> built = WaypointRows.filterAndSort(sourceRows, query);
			cachedRows.put(key, built);
			trimIfNeeded();
			return built;
		}
	}

	public void invalidateAll() {
		synchronized (cachedRows) {
			cachedRows.clear();
			sourceIdentity = null;
		}
	}

	private void ensureCurrentSource(Collection<NamedLocationView> sourceRows) {
		if (sourceIdentity == sourceRows) {
			return;
		}
		cachedRows.clear();
		sourceIdentity = sourceRows;
	}

	private void trimIfNeeded() {
		while (cachedRows.size() > maxCachedQueries) {
			WaypointListCacheKey eldestKey = cachedRows.keySet().iterator().next();
			cachedRows.remove(eldestKey);
		}
	}

}
