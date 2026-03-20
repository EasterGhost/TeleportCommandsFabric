package org.AndrewElizabeth.teleportcommandsfabric.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StorageManager {
	public static Path STORAGE_FOLDER;
	public static Path STORAGE_FILE;
	public static StorageClass STORAGE;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int defaultVersion = new StorageClass().getVersion();
	private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor();

	public static void StorageInit() {
		STORAGE_FOLDER = TeleportCommands.SAVE_DIR.resolve("TeleportCommands/");
		STORAGE_FILE = STORAGE_FOLDER.resolve("storage.json");

		try {
			StorageLoader();
		} catch (Exception e) {
			Constants.LOGGER.error("Error while initializing the storage file! Exiting! => ", e);
			throw new RuntimeException("Error while initializing the storage file! Exiting! => ", e);
		}
	}

	public static void StorageLoader() throws Exception {
		if (!STORAGE_FILE.toFile().exists() || STORAGE_FILE.toFile().length() == 0) {
			Constants.LOGGER.warn("Storage file was not found or was empty! Initializing storage");

			Files.createDirectories(STORAGE_FOLDER);
			STORAGE = new StorageClass();
			STORAGE.cleanup();
			saveStorageSync();
			Constants.LOGGER.info("Storage created successfully!");
			return;
		}

		StorageMigratorUtil.StorageMigrator(STORAGE_FILE, GSON, defaultVersion);

		try (BufferedReader reader = Files.newBufferedReader(STORAGE_FILE, StandardCharsets.UTF_8)) {
			STORAGE = GSON.fromJson(reader, StorageClass.class);
		}
		if (STORAGE == null) {
			Constants.LOGGER.warn("Storage file was empty! Initializing storage");
			STORAGE = new StorageClass();
			STORAGE.cleanup();
			saveStorageSync();
			return;
		}

		STORAGE.cleanup();

		saveStorageSync();
		Constants.LOGGER.info("Storage loaded successfully!");
	}

	public static void saveStorageSync() {
		try {
			if (STORAGE_FILE == null || STORAGE == null)
				return;
			byte[] json = GSON.toJson(STORAGE).getBytes(StandardCharsets.UTF_8);
			Path tempFile = STORAGE_FOLDER.resolve("storage.json.tmp");
			Files.write(tempFile, json, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.CREATE);
			Files.move(tempFile, STORAGE_FILE, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			Constants.LOGGER.error("Failed to save storage file synchronously!", e);
		}
	}

	private static volatile boolean isDirty = false;
	private static int ticksSinceLastSave = 0;

	public static void markDirty() {
		isDirty = true;
	}

	public static void tick() {
		if (!isDirty)
			return;
		ticksSinceLastSave++;

		int autoSaveTicks = 20 * ConfigManager.CONFIG.storage.getAutoSaveIntervalSeconds();
		if (ticksSinceLastSave >= autoSaveTicks) {
			StorageSaver();
			isDirty = false;
			ticksSinceLastSave = 0;
		}
	}

	public static void forceSaveOnShutdown() {
		Constants.LOGGER.info("Forcing synchronous save on server shutdown...");
		saveStorageSync();
		isDirty = false;
		ticksSinceLastSave = 0;
	}

	public static void StorageSaver() {
		try {
			if (STORAGE_FILE == null || STORAGE == null)
				return;
			final byte[] json = GSON.toJson(STORAGE).getBytes(StandardCharsets.UTF_8);
			IO_EXECUTOR.submit(() -> {
				try {
					Path tempFile = STORAGE_FOLDER.resolve("storage.json.tmp");
					Files.write(tempFile, json, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
							StandardOpenOption.CREATE);
					Files.move(tempFile, STORAGE_FILE, StandardCopyOption.REPLACE_EXISTING);
				} catch (Exception e) {
					Constants.LOGGER.error("Failed to save storage file asynchronously!", e);
				}
			});
		} catch (Exception e) {
			Constants.LOGGER.error("Failed to serialize storage file!", e);
		}
	}
}
