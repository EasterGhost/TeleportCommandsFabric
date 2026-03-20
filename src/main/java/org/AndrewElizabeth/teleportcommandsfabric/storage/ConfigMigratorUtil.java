package org.AndrewElizabeth.teleportcommandsfabric.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ConfigMigratorUtil {
	public static void ConfigMigrator(Path configFile, Gson gson, int defaultVersion) throws Exception {
		JsonObject jsonObject;
		try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			jsonObject = gson.fromJson(reader, JsonObject.class);
		}
		if (jsonObject == null) {
			jsonObject = new JsonObject();
		}

		int version = jsonObject.has("version") ? jsonObject.get("version").getAsInt() : 0;

		if (version < defaultVersion) {
			Constants.LOGGER.warn("Config file is v{}, migrating to v{}!", version, defaultVersion);

			if (jsonObject.has("wild") && !jsonObject.has("rtp")) {
				jsonObject.add("rtp", jsonObject.get("wild"));
				jsonObject.remove("wild");
			}

			normalizeXaeroSetNames(jsonObject);

			jsonObject.addProperty("version", defaultVersion);

			byte[] json = gson.toJson(jsonObject).getBytes(StandardCharsets.UTF_8);
			Files.write(configFile, json, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.CREATE);

			Constants.LOGGER.info("Config file migrated to v{} successfully!", defaultVersion);
		} else if (version > defaultVersion) {
			String message = String.format(
					"Teleport Commands: The config file's version is newer than the supported version, found v%s, expected <= v%s.\n"
							+
							"If you intentionally backported then you can attempt to downgrade the config file located at this location: \"%s\".\n",
					version, defaultVersion, configFile.toAbsolutePath());

			throw new IllegalStateException(message);
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

	private static void normalizeXaeroSetName(JsonObject xaero, String key, boolean warp) {
		if (!xaero.has(key) || xaero.get(key).isJsonNull()) {
			return;
		}

		String original = xaero.get(key).getAsString();
		String normalized = original == null ? "" : original.trim().toLowerCase();
		if (shouldFallbackToDefaultSet(normalized, warp)) {
			xaero.addProperty(key, "Default");
		}
	}

	private static boolean shouldFallbackToDefaultSet(String setName, boolean warp) {
		if (setName == null || setName.isBlank()) {
			return true;
		}

		if ("default".equals(setName) || "current".equals(setName)) {
			return true;
		}

		if (warp) {
			return "teleportcommands warps".equals(setName);
		}

		return "teleportcommands homes".equals(setName);
	}
}
