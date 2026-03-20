package org.AndrewElizabeth.teleportcommandsfabric.services;

import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.common.Player;

import java.util.Optional;
import java.util.UUID;

import static org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager.STORAGE;

public final class LocationResolver {
	private LocationResolver() {
	}

	public static String normalizeName(String name) {
		return name == null ? "" : name.toLowerCase();
	}

	public static Optional<NamedLocation> resolveHome(Player playerStorage, String homeName) {
		String normalizedHomeName = normalizeName(homeName);
		if (playerStorage == null || normalizedHomeName.isBlank()) {
			return Optional.empty();
		}
		return playerStorage.getHome(normalizedHomeName);
	}

	public static Optional<NamedLocation> resolveHome(Player playerStorage, UUID uuid) {
		if (playerStorage == null || uuid == null) {
			return Optional.empty();
		}
		return playerStorage.getHomeByUuid(uuid);
	}

	public static Optional<NamedLocation> resolveWarp(String warpName) {
		String normalizedWarpName = normalizeName(warpName);
		if (normalizedWarpName.isBlank()) {
			return Optional.empty();
		}
		return STORAGE.getWarp(normalizedWarpName);
	}

	public static Optional<NamedLocation> resolveWarp(UUID uuid) {
		if (uuid == null) {
			return Optional.empty();
		}
		return STORAGE.getWarpByUuid(uuid);
	}
}
