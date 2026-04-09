package org.AndrewElizabeth.teleportcommandsfabric.storage;
 
import com.google.gson.Gson;
import com.google.gson.JsonObject;
 
import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
 
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
 
public class ConfigMigrator {
	public static void migrate(Path configFile, Gson gson, int defaultVersion) throws Exception {
		JsonObject jsonObject;
		try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			jsonObject = gson.fromJson(reader, JsonObject.class);
		}
		if (jsonObject == null) {
			jsonObject = new JsonObject();
		}
 
		int version = jsonObject.has("version") ? jsonObject.get("version").getAsInt() : 0;
 
		if (version < defaultVersion) {
			ModConstants.LOGGER.warn("Config file is v{}, migrating to v{}!", version, defaultVersion);

			if (version < 2) {
				migrateToVersion2(jsonObject);
			}

			if (version < 3) {
				migrateToVersion3(jsonObject);
			}
 
			jsonObject.addProperty("version", defaultVersion);
 
			byte[] json = gson.toJson(jsonObject).getBytes(StandardCharsets.UTF_8);
			Files.write(configFile, json, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.CREATE);
 
			ModConstants.LOGGER.info("Config file migrated to v{} successfully!", defaultVersion);
		} else if (version > defaultVersion) {
			String message = String.format(
					"Teleport Commands: The config file's version is newer than the supported version, found v%s, expected <= v%s.\n"
							+
							"If you intentionally backported then you can attempt to downgrade the config file located at this location: \"%s\".\n",
					version, defaultVersion, configFile.toAbsolutePath());
 
			throw new IllegalStateException(message);
		}
	}

	private static void migrateToVersion2(JsonObject root) {
		if (root.has("wild") && !root.has("rtp")) {
			root.add("rtp", root.get("wild"));
			root.remove("wild");
		}

		normalizeXaeroSetNames(root);
	}

	private static void migrateToVersion3(JsonObject root) {
		if (!root.has("rtp") || !root.get("rtp").isJsonObject()) {
			return;
		}

		JsonObject rtp = root.getAsJsonObject("rtp");
		if (rtp.has("radius") && !rtp.has("maxRadius")) {
			rtp.add("maxRadius", rtp.get("radius"));
		}
		if (rtp.has("radius")) {
			rtp.remove("radius");
		}
	}
 
	private static void normalizeXaeroSetNames(JsonObject root) {
		if (!root.has("xaero") || !root.get("xaero").isJsonObject()) {
			return;
		}
 
		JsonObject xaero = root.getAsJsonObject("xaero");
		normalizeXaeroSetName(xaero, "warpSetName", true);
		normalizeXaeroSetName(xaero, "homeSetName", false);
	}
 
	private static void normalizeXaeroSetName(JsonObject xaero, String key, boolean isWarp) {
		if (!xaero.has(key) || xaero.get(key).isJsonNull()) {
			return;
		}
 
		String original = xaero.get(key).getAsString();
		String normalized = original == null ? "" : original.trim().toLowerCase();
		if (shouldFallbackToDefaultSet(normalized, isWarp)) {
			xaero.addProperty(key, "Default");
		}
	}
 
	private static boolean shouldFallbackToDefaultSet(String setName, boolean isWarp) {
		if (setName == null || setName.isBlank()) {
			return true;
		}
 
		if ("default".equals(setName) || "current".equals(setName)) {
			return true;
		}
 
		if (isWarp) {
			return "teleportcommands warps".equals(setName);
		}
 
		return "teleportcommands homes".equals(setName);
	}
}
