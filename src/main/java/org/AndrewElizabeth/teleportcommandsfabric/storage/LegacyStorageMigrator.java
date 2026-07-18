package org.AndrewElizabeth.teleportcommandsfabric.storage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfile;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileIO;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfile;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileIO;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class LegacyStorageMigrator {
	private static final Gson GSON = new Gson();
	private static final String LEGACY_STORAGE_FILE_NAME = "storage.json";
	private static final String MIGRATED_STORAGE_FILE_NAME = "storage.json.bak";

	private LegacyStorageMigrator() {
	}

	public static void migrateIfPresent() throws IOException {
		Path legacyStorageFile = getLegacyStorageFile();
		if (Files.notExists(legacyStorageFile) || Files.size(legacyStorageFile) == 0L) {
			return;
		}

		ModConstants.LOGGER.info("Legacy storage.json detected, starting migration to 2.0 NBT storage");

		JsonObject root = readLegacyStorageRoot(legacyStorageFile);
		MigrationData migrationData = buildMigrationData(root);
		writeMigrationData(migrationData);
		archiveLegacyStorageFile(legacyStorageFile);

		ModConstants.LOGGER.info(
				"Legacy storage migration completed: {} player profiles, {} global warps",
				migrationData.playerProfiles().size(),
				migrationData.globalProfile().getWarps().size());
	}

	private static JsonObject readLegacyStorageRoot(Path legacyStorageFile) throws IOException {
		try (Reader reader = Files.newBufferedReader(legacyStorageFile, StandardCharsets.UTF_8)) {
			JsonObject root = GSON.fromJson(reader, JsonObject.class);
			if (root == null) {
				throw new IOException("Legacy storage file is empty: " + legacyStorageFile);
			}
			return root;
		}
	}

	private static MigrationData buildMigrationData(JsonObject root) {
		GlobalProfile globalProfile = buildGlobalProfile(root.getAsJsonArray("Warps"));
		Map<UUID, PlayerProfile> playerProfiles = buildPlayerProfiles(root.getAsJsonArray("Players"));

		globalProfile.refreshWarpState();
		globalProfile.rebuildWarpNameIndex();

		Set<UUID> existingWarpUuids = globalProfile.getWarps().stream()
				.map(NamedLocation::getUuid)
				.collect(java.util.stream.Collectors.toSet());

		for (PlayerProfile playerProfile : playerProfiles.values()) {
			playerProfile.refreshHomeState();
			playerProfile.cleanupHiddenWarpUuids(existingWarpUuids);
			playerProfile.rebuildHomeNameIndex();
		}
		return new MigrationData(globalProfile, playerProfiles);
	}

	private static GlobalProfile buildGlobalProfile(JsonArray legacyWarps) {
		GlobalProfile globalProfile = new GlobalProfile();
		if (legacyWarps == null) {
			return globalProfile;
		}

		for (JsonElement element : legacyWarps) {
			if (!element.isJsonObject()) {
				continue;
			}

			parseLegacyNamedLocation(element.getAsJsonObject()).ifPresent(globalProfile::addWarp);
		}
		return globalProfile;
	}

	private static Map<UUID, PlayerProfile> buildPlayerProfiles(JsonArray legacyPlayers) {
		Map<UUID, PlayerProfile> playerProfiles = new LinkedHashMap<>();
		if (legacyPlayers == null) {
			return playerProfiles;
		}

		for (JsonElement element : legacyPlayers) {
			if (!element.isJsonObject()) {
				continue;
			}

			JsonObject legacyPlayer = element.getAsJsonObject();
			Optional<UUID> playerUuid = parseLegacyPlayerUuid(legacyPlayer);
			if (playerUuid.isEmpty()) {
				continue;
			}

			PlayerProfile profile = new PlayerProfile(playerUuid.get());
			List<UUID> hiddenWarpUuids = parseLegacyHiddenWarpUuids(legacyPlayer.getAsJsonArray("HiddenWarpUuids"));

			JsonArray legacyHomes = legacyPlayer.getAsJsonArray("Homes");
			if (legacyHomes != null) {
				for (JsonElement homeElement : legacyHomes) {
					if (!homeElement.isJsonObject()) {
						continue;
					}

					parseLegacyNamedLocation(homeElement.getAsJsonObject()).ifPresent(profile::addHome);
				}
			}

			Optional<UUID> defaultHomeUuid = parseUuid(getString(legacyPlayer, "DefaultHomeUuid"));
			if (defaultHomeUuid.isPresent()) {
				profile.setDefaultHome(defaultHomeUuid.get());
			} else {
				String defaultHomeName = getString(legacyPlayer, "DefaultHome");
				if (!defaultHomeName.isBlank()) {
					profile.setDefaultHomeByName(defaultHomeName);
				}
			}

			for (UUID hiddenWarpUuid : hiddenWarpUuids) {
				profile.hideWarp(hiddenWarpUuid);
			}

			if (!profile.isEmpty()) {
				playerProfiles.put(playerUuid.get(), profile);
			}
		}
		return playerProfiles;
	}

	private static Optional<NamedLocation> parseLegacyNamedLocation(JsonObject legacyLocation) {
		String name = getString(legacyLocation, "name");
		String dimensionId = getString(legacyLocation, "world");
		if (name.isBlank() || dimensionId.isBlank()) {
			return Optional.empty();
		}

		Optional<UUID> uuid = parseUuid(getString(legacyLocation, "uuid"));
		Optional<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> dimension = WorldResolver.getDimensionById(dimensionId);
		if (dimension.isEmpty()) {
			return Optional.empty();
		}

		int x = getInt(legacyLocation, "x", 0);
		double y = getDouble(legacyLocation, "y", 0D);
		int z = getInt(legacyLocation, "z", 0);
		boolean visible = getBooleanOr(legacyLocation, "xaeroVisible", true);
		long expiredTime = getLong(legacyLocation, "expiredTime", 0L);

		return Optional.of(new NamedLocation(uuid.orElse(null), name, x, y, z, dimension.get(), visible, expiredTime));
	}

	private static Optional<UUID> parseLegacyPlayerUuid(JsonObject legacyPlayer) {
		String uuid = getString(legacyPlayer, "UUID");
		if (uuid.isBlank()) {
			uuid = getString(legacyPlayer, "Player_UUID");
		}
		return parseUuid(uuid);
	}

	private static List<UUID> parseLegacyHiddenWarpUuids(JsonArray legacyHiddenWarpUuids) {
		List<UUID> hiddenWarpUuids = new ArrayList<>();
		if (legacyHiddenWarpUuids == null) {
			return hiddenWarpUuids;
		}

		for (JsonElement element : legacyHiddenWarpUuids) {
			if (!element.isJsonPrimitive()) {
				continue;
			}

			parseUuid(element.getAsString()).ifPresent(hiddenWarpUuids::add);
		}

		return hiddenWarpUuids;
	}

	private static void writeMigrationData(MigrationData migrationData) throws IOException {
		GlobalProfileIO globalProfileIO = new GlobalProfileIO();
		PlayerProfileIO playerProfileIO = new PlayerProfileIO();

		if (migrationData.globalProfile().isEmpty()) {
			globalProfileIO.delete();
		} else {
			globalProfileIO.save(migrationData.globalProfile());
		}

		for (PlayerProfile profile : migrationData.playerProfiles().values()) {
			playerProfileIO.save(profile);
		}
	}

	private static void archiveLegacyStorageFile(Path legacyStorageFile) throws IOException {
		Files.move(
				legacyStorageFile,
				legacyStorageFile.resolveSibling(MIGRATED_STORAGE_FILE_NAME),
				StandardCopyOption.REPLACE_EXISTING);
	}

	private static Path getLegacyStorageFile() throws IOException {
		if (TeleportCommands.SAVE_DIR == null) {
			throw new IOException("SAVE_DIR is not initialized");
		}

		return TeleportCommands.SAVE_DIR.resolve("TeleportCommands").resolve(LEGACY_STORAGE_FILE_NAME);
	}

	private static String getString(JsonObject object, String key) {
		if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
			return "";
		}

		JsonElement element = object.get(key);
		if (!element.isJsonPrimitive()) {
			return "";
		}

		try {
			return element.getAsString().trim();
		} catch (Exception exception) {
			return "";
		}
	}

	private static int getInt(JsonObject object, String key, int fallback) {
		if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
			return fallback;
		}

		try {
			return object.get(key).getAsInt();
		} catch (Exception exception) {
			return fallback;
		}
	}

	private static double getDouble(JsonObject object, String key, double fallback) {
		if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
			return fallback;
		}

		try {
			return object.get(key).getAsDouble();
		} catch (Exception exception) {
			return fallback;
		}
	}

	private static long getLong(JsonObject object, String key, long fallback) {
		if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
			return fallback;
		}

		try {
			return object.get(key).getAsLong();
		} catch (Exception exception) {
			return fallback;
		}
	}

	private static boolean getBooleanOr(JsonObject object, String key, boolean fallback) {
		if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
			return fallback;
		}

		try {
			return object.get(key).getAsBoolean();
		} catch (Exception exception) {
			return fallback;
		}
	}

	private static Optional<UUID> parseUuid(String value) {
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}

		try {
			return Optional.of(UUID.fromString(value));
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}

	private record MigrationData(GlobalProfile globalProfile, Map<UUID, PlayerProfile> playerProfiles) {
	}
}
