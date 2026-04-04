package org.AndrewElizabeth.teleportcommandsfabric.commands.common;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandHelper;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DimensionFilterCommandSupport {
	private static final Map<String, String> DIMENSION_ALIASES = Map.of(
			"overworld", "minecraft:overworld",
			"nether", "minecraft:the_nether",
			"end", "minecraft:the_end");

	private static final Comparator<NamedLocation> NAME_COMPARATOR = Comparator
			.comparing((NamedLocation location) -> location.getName().toLowerCase(Locale.ROOT))
			.thenComparing(NamedLocation::getName)
			.thenComparing(location -> location.getUuid().toString());

	private DimensionFilterCommandSupport() {
	}

	public static String normalizeDimensionFilter(String dimensionFilter) {
		if (dimensionFilter == null) {
			return null;
		}

		String normalized = dimensionFilter.trim().toLowerCase(Locale.ROOT);
		if (normalized.isBlank()) {
			return null;
		}

		return DIMENSION_ALIASES.getOrDefault(normalized, normalized);
	}

	public static List<String> getDimensionSuggestions() {
		Set<String> suggestions = new LinkedHashSet<>(DIMENSION_ALIASES.keySet());
		if (TeleportCommands.SERVER != null) {
			for (ServerLevel level : TeleportCommands.SERVER.getAllLevels()) {
				suggestions.add(WorldResolver.getDimensionId(level.dimension()));
			}
		}
		return new ArrayList<>(suggestions);
	}

	public static List<NamedLocation> sortAndFilter(List<NamedLocation> entries, String dimensionFilter) {
		String normalizedDimensionFilter = normalizeDimensionFilter(dimensionFilter);
		return entries.stream()
				.sorted(NAME_COMPARATOR)
				.filter(location -> normalizedDimensionFilter == null
						|| Objects.equals(normalizedDimensionFilter, location.getWorldString()))
				.toList();
	}

	public static String buildPageCommand(String baseCommand, int page, String dimensionFilter) {
		return baseCommand + " " + page + buildDimensionArgument(dimensionFilter);
	}

	public static String buildDimensionArgument(String dimensionFilter) {
		String normalizedDimensionFilter = normalizeDimensionFilter(dimensionFilter);
		if (normalizedDimensionFilter == null) {
			return "";
		}

		return " " + CommandHelper.quoteCommandArgument(normalizedDimensionFilter);
	}
}
