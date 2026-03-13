package org.AndrewElizabeth.teleportcommandsfabric.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.common.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.common.Player;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.unmodifiableList;

public class StorageManager {
	public static Path STORAGE_FOLDER;
	public static Path STORAGE_FILE;
	public static StorageClass STORAGE;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int defaultVersion = new StorageClass().getVersion();

	/// Initializes the StorageManager class and loads the storage from
	/// the filesystem.
	public static void StorageInit() {
		STORAGE_FOLDER = TeleportCommands.SAVE_DIR.resolve("TeleportCommands/");
		STORAGE_FILE = STORAGE_FOLDER.resolve("storage.json");

		try {
			StorageLoader();
		} catch (Exception e) {
			// crashing is probably better here, otherwise the whole mod will be broken
			Constants.LOGGER.error("Error while initializing the storage file! Exiting! => ", e);
			throw new RuntimeException("Error while initializing the storage file! Exiting! => ", e);
		}
	}

	/// Loads the storage from the filesystem
	public static void StorageLoader() throws Exception {
		if (!STORAGE_FILE.toFile().exists() || STORAGE_FILE.toFile().length() == 0) {
			Constants.LOGGER.warn("Storage file was not found or was empty! Initializing storage");

			Files.createDirectories(STORAGE_FOLDER);
			STORAGE = new StorageClass();
			StorageSaver();
			Constants.LOGGER.info("Storage created successfully!");
		}

		StorageMigrator();

		try (BufferedReader reader = Files.newBufferedReader(STORAGE_FILE, StandardCharsets.UTF_8)) {
			STORAGE = GSON.fromJson(reader, StorageClass.class);
		}
		if (STORAGE == null) {
			Constants.LOGGER.warn("Storage file was empty! Initializing storage");
			STORAGE = new StorageClass();
			StorageSaver();
		}

		STORAGE.cleanup();

		StorageSaver(); // Save it so any missing values get added to the file.
		Constants.LOGGER.info("Storage loaded successfully!");
	}

	/// This function checks what version the storage file is and migrates it to the
	/// current version of the mod.
	public static void StorageMigrator() throws Exception {
		JsonObject jsonObject;
		try (BufferedReader reader = Files.newBufferedReader(STORAGE_FILE, StandardCharsets.UTF_8)) {
			jsonObject = GSON.fromJson(reader, JsonObject.class);
		}
		if (jsonObject == null) {
			jsonObject = new JsonObject();
		}

		int version = jsonObject.has("version") ? jsonObject.get("version").getAsInt() : 0;

		if (version < defaultVersion) {
			Constants.LOGGER.warn("Storage file is v{}, migrating to v{}!", version, defaultVersion);

			// In v1.1.0 "Player_UUID" got renamed to "UUID". Since the storage file didn't
			// have a version yet, it is set to version 0.
			if (version == 0) {

				if (jsonObject.has("Players") && jsonObject.get("Players").isJsonArray()) {

					JsonArray players = jsonObject.get("Players").getAsJsonArray();

					for (int i = players.size() - 1; i >= 0; i--) {
						JsonObject player = players.get(i).getAsJsonObject();

						String UUID = player.has("Player_UUID")
								? player.get("Player_UUID").getAsString()
								: (player.has("UUID")
										? player.get("UUID").getAsString()
										: null);

						if (UUID == null || UUID.isBlank()) {
							// remove it then, it's an invalid entry 0.0
							players.remove(i);

						} else {
							player.remove("Player_UUID");
							player.addProperty("UUID", UUID);
						}
					}
				}

			}

			// In v2.0.0 NamedLocation.y switched to precise double-based storage.
			if (version < Constants.STORAGE_VERSION) {
				normalizeNamedLocationYAsDouble(jsonObject);
				ensureNamedLocationXaeroVisible(jsonObject);
				ensureNamedLocationUuid(jsonObject);
				migrateDefaultHomeToUuid(jsonObject);
			}

			// Always bump to the latest supported schema version after migrations.
			jsonObject.addProperty("version", defaultVersion);

			// Save the storage :3
			byte[] json = GSON.toJson(jsonObject).getBytes(StandardCharsets.UTF_8);
			Files.write(STORAGE_FILE, json, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.CREATE);

			Constants.LOGGER.info("Storage file migrated to v{} successfully!", defaultVersion);
		} else if (version > defaultVersion) {
			String message = String.format(
					"Teleport Commands: The storage file's version is newer than the supported version, found v%s, expected <= v%s.\n"
							+
							"If you intentionally backported then you can attempt to downgrade the storage file located at this location: \"%s\".\n",
					version, defaultVersion, STORAGE_FILE.toAbsolutePath());

			throw new IllegalStateException(message);
		}
	}

	/// Saves the storage to the filesystem
	public static void StorageSaver() throws Exception {
		byte[] json = GSON.toJson(StorageManager.STORAGE).getBytes(StandardCharsets.UTF_8);

		Files.write(STORAGE_FILE, json, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.CREATE);
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

			player.addProperty("DefaultHomeUuid", resolvedUuid);
			player.remove("DefaultHome");
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

	public static class StorageClass {
		private final int version = Constants.STORAGE_VERSION;
		private final ArrayList<NamedLocation> Warps = new ArrayList<>();
		private final ArrayList<Player> Players = new ArrayList<>();

		/// Cleans up any values in the storage class
		public void cleanup() throws Exception {
			boolean changed = false;
			for (Iterator<Player> iterator = Players.iterator(); iterator.hasNext();) {
				Player player = iterator.next();

				// Remove null/corrupt player entries from malformed storage files.
				if (player == null) {
					iterator.remove();
					continue;
				}

				// Remove players with invalid UUID's
				String uuid = player.getUUID();
				if (uuid == null || uuid.isBlank()) {
					iterator.remove();
					continue;
				}

				// Delete any homes with an invalid world_id (if enabled in config)
				if (ConfigManager.CONFIG.home.isDeleteInvalid()) {
					List<NamedLocation> homesSnapshot = new ArrayList<>(player.getHomes());
					for (NamedLocation home : homesSnapshot) {
						changed |= home.ensureUuid();
						if (home.getWorld().isEmpty()) {
							player.deleteHomeNoSave(home);
							changed = true;
						}
					}

					changed |= player.ensureDefaultHomeUuid();
				}

				if (!ConfigManager.CONFIG.home.isDeleteInvalid()) {
					for (NamedLocation home : player.getHomes()) {
						changed |= home.ensureUuid();
					}
					changed |= player.ensureDefaultHomeUuid();
				}

				// Remove players with no homes
				if (player.getHomes().isEmpty()) {
					iterator.remove();
					changed = true;
				}
			}

			// Delete any warps with an invalid world_id (if enabled in config)
			for (NamedLocation warp : Warps) {
				changed |= warp.ensureUuid();
			}
			if (ConfigManager.CONFIG.warp.isDeleteInvalid()) {
				boolean removed = Warps.removeIf(warp -> warp.getWorld().isEmpty());
				changed |= removed;
			}

			if (changed) {
				StorageSaver();
			}
		}

		public int getVersion() {
			return version;
		}

		// returns all warps
		public List<NamedLocation> getWarps() {
			return unmodifiableList(Warps);
		}

		// filters the warpList and finds the one with the name (if there is one)
		public Optional<NamedLocation> getWarp(String name) {
			return Warps.stream()
					.filter(warp -> Objects.equals(warp.getName(), name))
					.findFirst();
		}

		public Optional<NamedLocation> getWarpByUuid(UUID uuid) {
			return Warps.stream()
					.filter(warp -> Objects.equals(warp.getUuid(), uuid))
					.findFirst();
		}

		// filters the playerList and finds the one with the uuid (if there is one)
		public Optional<Player> getPlayer(String uuid) {
			return Players.stream()
					.filter(player -> Objects.equals(player.getUUID(), uuid))
					.findFirst();
		}

		// -----

		// Adds a NamedLocation to the warp list, returns true if a warp with the same
		// name already exists
		public boolean addWarp(NamedLocation warp) throws Exception {
			if (getWarp(warp.getName()).isPresent()) {
				// Warp with same name found!
				return true;

			} else {
				Warps.add(warp);
				StorageSaver();
				return false;
			}
		}

		// Creates a new player, if there already is a player it will return the
		// existing one. The player won't be saved unless they actually do something lol
		// The name of this function is wack but whatever kewk
		public Player addPlayer(String uuid) {
			final Optional<Player> OptionalPlayer = getPlayer(uuid);

			if (OptionalPlayer.isEmpty()) {
				// create and return new player
				Player player = new Player(uuid);
				Players.add(player);

				return player;
			} else {
				// return existing player
				return OptionalPlayer.get();
			}
		}

		// -----

		// Remove a warp, if the warp isn't found then nothing will happen
		public void removeWarp(NamedLocation warp) throws Exception {
			Warps.removeIf(existing -> Objects.equals(existing.getUuid(), warp.getUuid()));
			StorageSaver();
		}
	}
}
