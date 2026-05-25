package org.AndrewElizabeth.teleportcommandsfabric.ui.cli;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.SortDirection;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.WaypointListQuery;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.WaypointSortKey;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class WaypointRows {
	private WaypointRows() {
	}

	public static List<NamedLocationView> filterAndSort(Collection<NamedLocationView> rows, WaypointListQuery query) {
		return rows.stream().filter(query.filter()::matches).sorted(comparator(query)).toList();
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

	private static Comparator<NamedLocationView> primaryComparator(WaypointSortKey sortKey) {
		return switch (sortKey) {
		case NAME -> Comparator.comparing(location -> normalize(location.getName()));
		case SEQUENCE -> Comparator.comparingInt(NamedLocationView::getSequence);
		};
	}

	private static String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT);
	}
}
