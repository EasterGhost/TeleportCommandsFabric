package org.AndrewElizabeth.teleportcommandsfabric.storage.global;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationSnapshot;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

record GlobalProfileSnapshot(List<NamedLocationView> warps, Map<String, NamedLocationView> warpsByName,
		Map<UUID, NamedLocationView> warpsByUuid) implements GlobalProfileView {
	static GlobalProfileSnapshot from(GlobalProfile profile) {
		List<NamedLocationView> warps = NamedLocationSnapshot.list(profile.getWarps());
		Map<String, NamedLocationView> warpsByName = new HashMap<>();
		Map<UUID, NamedLocationView> warpsByUuid = new HashMap<>();
		for (NamedLocationView warp : warps) {
			warpsByName.put(normalizeName(warp.getName()), warp);
			warpsByUuid.put(warp.getUuid(), warp);
		}
		return new GlobalProfileSnapshot(List.copyOf(warps), Map.copyOf(warpsByName), Map.copyOf(warpsByUuid));
	}

	@Override
	public List<NamedLocationView> getWarps() {
		return warps;
	}

	@Override
	public Optional<NamedLocationView> getWarpByName(String name) {
		return Optional.ofNullable(warpsByName.get(normalizeName(name)))
				.filter(warp -> !warp.isExpired());
	}

	@Override
	public Optional<NamedLocationView> getWarp(UUID uuid) {
		return Optional.ofNullable(warpsByUuid.get(uuid))
				.filter(warp -> !warp.isExpired());
	}

	private static String normalizeName(String name) {
		return name == null ? "" : name.toLowerCase(Locale.ROOT);
	}
}
