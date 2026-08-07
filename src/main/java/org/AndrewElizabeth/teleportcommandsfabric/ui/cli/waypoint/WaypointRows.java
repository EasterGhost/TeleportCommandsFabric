package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.SortDirection;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.SortKey;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class WaypointRows {
	private WaypointRows() {
	}

	public static List<NamedLocationView> filterAndSort(Collection<NamedLocationView> rows, WaypointListQuery query) {
		return rows.stream().filter(query.filter()::matches).sorted(comparator(query)).toList();
	}

	public static Optional<NamedLocationView> findByUuid(Collection<NamedLocationView> rows, UUID waypointUuid) {
		if (rows == null || waypointUuid == null) {
			return Optional.empty();
		}
		return rows.stream()
				.filter(location -> waypointUuid.equals(location.getUuid()))
				.findFirst();
	}

	private static Comparator<NamedLocationView> comparator(WaypointListQuery query) {
		Comparator<NamedLocationView> primary = primaryComparator(query.sort().key());
		if (query.sort().direction() == SortDirection.DESC) {
			primary = primary.reversed();
		}
		return primary.thenComparingInt(NamedLocationView::getSequence)
				.thenComparing(location -> normalize(location.getName()))
				.thenComparing(location -> location.getUuid().toString());
	}

	private static Comparator<NamedLocationView> primaryComparator(SortKey sortKey) {
		return switch (sortKey) {
		case NAME -> Comparator.comparing(location -> normalize(location.getName()));
		case SEQUENCE -> Comparator.comparingInt(NamedLocationView::getSequence);
		};
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}
}
