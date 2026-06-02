package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;

import java.util.Locale;

public sealed interface WaypointFilter permits WaypointFilter.None, WaypointFilter.Prefix, WaypointFilter.Dimension {
	WaypointFilter NONE = new None();

	boolean matches(NamedLocationView location);

	default boolean isNone() {
		return this instanceof None;
	}

	static WaypointFilter none() {
		return NONE;
	}

	static WaypointFilter prefix(String value) {
		String normalized = normalize(value);
		return normalized.isEmpty() ? NONE : new Prefix(normalized.substring(0, 1));
	}

	static WaypointFilter dimension(String dimensionId) {
		String normalized = normalize(dimensionId);
		return normalized.isEmpty() ? NONE : new Dimension(normalized);
	}

	static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}

	record None() implements WaypointFilter {
		@Override
		public boolean matches(NamedLocationView location) {
			return true;
		}
	}

	record Prefix(String value) implements WaypointFilter {
		public Prefix {
			value = normalize(value);
			if (value.isEmpty()) {
				throw new IllegalArgumentException("prefix filter value cannot be empty");
			}
			if (value.length() > 1) {
				value = value.substring(0, 1);
			}
		}

		@Override
		public boolean matches(NamedLocationView location) {
			return normalize(location.getName()).startsWith(value);
		}
	}

	record Dimension(String dimensionId) implements WaypointFilter {
		public Dimension {
			dimensionId = normalize(dimensionId);
			if (dimensionId.isEmpty()) {
				throw new IllegalArgumentException("dimension filter value cannot be empty");
			}
		}

		@Override
		public boolean matches(NamedLocationView location) {
			return normalize(location.getDimensionId()).equals(dimensionId);
		}
	}
}
