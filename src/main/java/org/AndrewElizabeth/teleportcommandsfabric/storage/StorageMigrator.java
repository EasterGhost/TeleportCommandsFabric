package org.AndrewElizabeth.teleportcommandsfabric.storage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class StorageMigrator {

	public static void migrate(Path storageFile, Gson gson, int defaultVersion) throws Exception {
		JsonObject jsonObject;
		try (BufferedReader reader = Files.newBufferedReader(storageFile, StandardCharsets.UTF_8)) {
			jsonObject = gson.fromJson(reader, JsonObject.class);
		}
		if (jsonObject == null) {
			jsonObject = new JsonObject();
		}

		int version = jsonObject.has("version") ? jsonObject.get("version").getAsInt() : 0;

		if (version < defaultVersion) {
			ModConstants.LOGGER.warn("Storage file is v{}, migrating to v{}!", version, defaultVersion);

			if (version == 0) {
				if (jsonObject.has("Players") && jsonObject.get("Players").isJsonArray()) {
					JsonArray players = jsonObject.get("Players").getAsJsonArray();

					for (int i = players.size() - 1; i >= 0; i--) {
						JsonObject player = players.get(i).getAsJsonObject();

						String UUIDStr = player.has("Player_UUID")
								? player.get("Player_UUID").getAsString()
								: (player.has("UUID")
										? player.get("UUID").getAsString()
										: null);

						if (UUIDStr == null || UUIDStr.isBlank()) {
							players.remove(i);
						} else {
							player.remove("Player_UUID");
							player.addProperty("UUID", UUIDStr);
						}
					}
				}
			}

			if (version < 3) {
				migrateToVersion3(jsonObject);
			}

			if (version < 4) {
				migrateHistoricalVersion3DataToVersion4(jsonObject);
			}

			jsonObject.addProperty("version", defaultVersion);

			byte[] json = gson.toJson(jsonObject).getBytes(StandardCharsets.UTF_8);
			Files.write(storageFile, json, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.CREATE);

			ModConstants.LOGGER.info("Storage file migrated to v{} successfully!", defaultVersion);
		} else if (version > defaultVersion) {
			String message = String.format(
					"Teleport Commands: The storage file's version is newer than the supported version, found v%s, expected <= v%s.\n"
							+
							"If you intentionally backported then you can attempt to downgrade the storage file located at this location: \"%s\".\n",
					version, defaultVersion, storageFile.toAbsolutePath());

			throw new IllegalStateException(message);
		}
	}

	private static void migrateToVersion3(JsonObject root) {
		normalizeNamedLocationYAsDouble(root);
		ensureNamedLocationXaeroVisible(root);
	}

	private static void migrateHistoricalVersion3DataToVersion4(JsonObject root) {
		ensureNamedLocationUuid(root);
		migrateDefaultHomeToUuid(root);
		ensurePlayerHiddenWarpUuids(root);
	}

	private static void normalizeNamedLocationYAsDouble(JsonObject root) {
		normalizeLocationsArrayY(root.getAsJsonArray("Warps"));

		if (root.has("Players") && root.get("Players").isJsonArray()) {
			JsonArray players = root.getAsJsonArray("Players");
			for (JsonElement element : players) {
				if (!element.isJsonObject()) {
					continue;
				}
				JsonObject player = element.getAsJsonObject();
				normalizeLocationsArrayY(player.getAsJsonArray("Homes"));
			}
		}
	}

	private static void ensureNamedLocationXaeroVisible(JsonObject root) {
		ensureLocationsArrayXaeroVisible(root.getAsJsonArray("Warps"));

		if (root.has("Players") && root.get("Players").isJsonArray()) {
			JsonArray players = root.getAsJsonArray("Players");
			for (JsonElement element : players) {
				if (!element.isJsonObject()) {
					continue;
				}
				JsonObject player = element.getAsJsonObject();
				ensureLocationsArrayXaeroVisible(player.getAsJsonArray("Homes"));
			}
		}
	}

	private static void ensureNamedLocationUuid(JsonObject root) {
		ensureLocationsArrayUuid(root.getAsJsonArray("Warps"));

		if (root.has("Players") && root.get("Players").isJsonArray()) {
			JsonArray players = root.getAsJsonArray("Players");
			for (JsonElement element : players) {
				if (!element.isJsonObject()) {
					continue;
				}
				JsonObject player = element.getAsJsonObject();
				ensureLocationsArrayUuid(player.getAsJsonArray("Homes"));
			}
		}
	}

	private static void ensureLocationsArrayUuid(JsonArray locations) {
		if (locations == null) {
			return;
		}
		for (JsonElement element : locations) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject location = element.getAsJsonObject();
			if (!location.has("uuid") || location.get("uuid").isJsonNull()
					|| location.get("uuid").getAsString().isBlank()) {
				location.addProperty("uuid", UUID.randomUUID().toString());
			}
		}
	}

	private static void migrateDefaultHomeToUuid(JsonObject root) {
		if (!root.has("Players") || !root.get("Players").isJsonArray()) {
			return;
		}

		JsonArray players = root.getAsJsonArray("Players");
		for (JsonElement element : players) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject player = element.getAsJsonObject();

			String defaultHomeUuid = player.has("DefaultHomeUuid") && !player.get("DefaultHomeUuid").isJsonNull()
					? player.get("DefaultHomeUuid").getAsString()
					: "";
			if (!defaultHomeUuid.isBlank()) {
				player.remove("DefaultHome");
				continue;
			}

			String defaultHomeName = player.has("DefaultHome") && !player.get("DefaultHome").isJsonNull()
					? player.get("DefaultHome").getAsString()
					: "";
			String resolvedUuid = "";

			if (!defaultHomeName.isBlank() && player.has("Homes") && player.get("Homes").isJsonArray()) {
				JsonArray homes = player.getAsJsonArray("Homes");
				for (JsonElement homeElement : homes) {
					if (!homeElement.isJsonObject()) {
						continue;
					}
					JsonObject home = homeElement.getAsJsonObject();
					String name = home.has("name") && !home.get("name").isJsonNull() ? home.get("name").getAsString() : "";
					if (defaultHomeName.equals(name)) {
						resolvedUuid = home.has("uuid") && !home.get("uuid").isJsonNull()
								? home.get("uuid").getAsString()
								: "";
						break;
					}
				}
			}

			if (resolvedUuid.isBlank()) {
				player.remove("DefaultHomeUuid");
			} else {
				player.addProperty("DefaultHomeUuid", resolvedUuid);
			}
			player.remove("DefaultHome");
		}
	}

	private static void ensurePlayerHiddenWarpUuids(JsonObject root) {
		if (!root.has("Players") || !root.get("Players").isJsonArray()) {
			return;
		}

		JsonArray players = root.getAsJsonArray("Players");
		for (JsonElement element : players) {
			if (!element.isJsonObject()) {
				continue;
			}

			JsonObject player = element.getAsJsonObject();
			if (!player.has("HiddenWarpUuids") || player.get("HiddenWarpUuids").isJsonNull()) {
				player.add("HiddenWarpUuids", new JsonArray());
			}
		}
	}

	private static void ensureLocationsArrayXaeroVisible(JsonArray locations) {
		if (locations == null) {
			return;
		}
		for (JsonElement element : locations) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject location = element.getAsJsonObject();
			if (!location.has("xaeroVisible")) {
				location.addProperty("xaeroVisible", true);
			}
		}
	}

	private static void normalizeLocationsArrayY(JsonArray locations) {
		if (locations == null) {
			return;
		}
		for (JsonElement element : locations) {
			if (!element.isJsonObject()) {
				continue;
			}
			JsonObject location = element.getAsJsonObject();
			if (!location.has("y")) {
				continue;
			}

			JsonElement yValue = location.get("y");
			if (!yValue.isJsonPrimitive() || !yValue.getAsJsonPrimitive().isNumber()) {
				continue;
			}

			location.addProperty("y", yValue.getAsDouble());
		}
	}
}
