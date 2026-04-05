package org.AndrewElizabeth.teleportcommandsfabric.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfigManager {
	public static Path CONFIG_FILE;
	public static ConfigClass CONFIG;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor();

	public static void ConfigInit() {
		CONFIG_FILE = TeleportCommands.CONFIG_DIR.resolve("teleport_commands.json");

		try {
			ConfigLoader();

		} catch (Exception e) {
			ModConstants.LOGGER.error("Error while initializing the config file! Exiting! => ", e);
			throw new RuntimeException("Error while initializing the config file! Exiting! => ", e);
		}
	}

	public static void ConfigLoader() throws Exception {
		if (!CONFIG_FILE.toFile().exists() || CONFIG_FILE.toFile().length() == 0) {
			Files.createDirectories(TeleportCommands.CONFIG_DIR);

			ModConstants.LOGGER.warn("Config file was not found or was empty! Initializing config");
			CONFIG = new ConfigClass();
			saveConfigSync();
			ModConstants.LOGGER.info("Config created successfully!");
			return;
		}

		ConfigMigrator();

		try (BufferedReader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
			CONFIG = GSON.fromJson(reader, ConfigClass.class);
		}
		if (CONFIG == null) {
			ModConstants.LOGGER.warn("Config file was empty! Loading defaults...");
			CONFIG = new ConfigClass();
			saveConfigSync();
			return;
		}

		saveConfigSync();
		ModConstants.LOGGER.info("Config loaded successfully!");
	}

	private static void ConfigMigrator() throws Exception {
		try (BufferedReader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
			JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
			int version = jsonObject.has("version") ? jsonObject.get("version").getAsInt() : 0;
			if (version < ModConstants.CONFIG_VERSION) {
				ConfigMigrator.migrate(CONFIG_FILE, GSON, ModConstants.CONFIG_VERSION);
			}
		}
	}

	public static void saveConfigSync() {
		if (CONFIG_FILE == null || CONFIG == null) {
			ModConstants.LOGGER.error("Cannot save config: CONFIG_FILE or CONFIG is null.");
			return;
		}
		try {
			byte[] json = GSON.toJson(CONFIG).getBytes(StandardCharsets.UTF_8);
			Path tempFile = TeleportCommands.CONFIG_DIR.resolve("teleport_commands.json.tmp");
			Files.write(tempFile, json, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.CREATE);
			Files.move(tempFile, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			ModConstants.LOGGER.error("Error while saving the config file! => ", e);
		}
	}

	public static void ConfigSaver() {
		try {
			if (CONFIG_FILE == null || CONFIG == null)
				return;
			final byte[] json = GSON.toJson(CONFIG).getBytes(StandardCharsets.UTF_8);
			IO_EXECUTOR.submit(() -> {
				try {
					Path tempFile = TeleportCommands.CONFIG_DIR.resolve("teleport_commands.json.tmp");
					Files.write(tempFile, json, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
							StandardOpenOption.CREATE);
					Files.move(tempFile, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING);
				} catch (Exception e) {
					ModConstants.LOGGER.error("Error while saving the config file asynchronously! => ", e);
				}
			});
		} catch (Exception e) {
			ModConstants.LOGGER.error("Failed to serialize config file!", e);
		}
	}
}
