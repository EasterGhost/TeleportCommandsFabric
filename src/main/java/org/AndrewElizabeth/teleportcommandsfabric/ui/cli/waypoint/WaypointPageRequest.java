package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public record WaypointPageRequest(
		WaypointPageKind kind,
		List<NamedLocationView> locations,
		Set<UUID> hiddenWarpUuids,
		UUID defaultLocationUuid,
		boolean canModify,
		Set<UUID> publishedHomeUuids,
		WaypointListQuery query,
		String language) {
	public WaypointPageRequest(WaypointPageKind kind, List<NamedLocationView> locations, Set<UUID> hiddenWarpUuids,
			UUID defaultLocationUuid, boolean canModify, WaypointListQuery query, String language) {
		this(kind, locations, hiddenWarpUuids, defaultLocationUuid, canModify, Set.of(), query, language);
	}

	public WaypointPageRequest {
		kind = kind == null ? WaypointPageKind.HOMES : kind;
		locations = locations == null ? List.of() : List.copyOf(locations);
		hiddenWarpUuids = hiddenWarpUuids == null ? Set.of() : Set.copyOf(hiddenWarpUuids);
		publishedHomeUuids = publishedHomeUuids == null ? Set.of() : Set.copyOf(publishedHomeUuids);
		query = query == null ? WaypointListQuery.defaultQuery() : query;
		language = language == null || language.isBlank() ? "en_us" : language.toLowerCase(Locale.ROOT);
	}
}
