package org.AndrewElizabeth.teleportcommandsfabric.storage;

import com.google.gson.*;
import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ConfigManager {
	public static Path CONFIG_FILE;
	public static ConfigClass CONFIG;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int defaultVersion = new ConfigClass().getVersion();

	public static void ConfigInit() {
		CONFIG_FILE = TeleportCommands.CONFIG_DIR.resolve("teleport_commands.json");

		try {
			ConfigLoader();

		} catch (Exception e) {
			// crashing is probably better here, otherwise the whole mod will be broken
			Constants.LOGGER.error("Error while initializing the config file! Exiting! => ", e);
			throw new RuntimeException("Error while initializing the config file! Exiting! => ", e);
		}
	}

	public static void ConfigLoader() throws Exception {
		if (!CONFIG_FILE.toFile().exists() || CONFIG_FILE.toFile().length() == 0) {
			Files.createDirectories(TeleportCommands.CONFIG_DIR);

			Constants.LOGGER.warn("Config file was not found or was empty! Initializing config");
			CONFIG = new ConfigClass();
			ConfigSaver();
			Constants.LOGGER.info("Config created successfully!");
		}

		ConfigMigrator();

		FileReader reader = new FileReader(CONFIG_FILE.toFile());
		CONFIG = GSON.fromJson(reader, ConfigClass.class);
		if (CONFIG == null) {
			Constants.LOGGER.warn("Config file was empty! Loading defaults...");
			CONFIG = new ConfigClass();
			ConfigSaver();
		}

		ConfigSaver(); // Save it so any missing values get added to the file.
		Constants.LOGGER.info("Config loaded successfully!");
	}

	/// This function checks what version the config file is and migrates it to the
	/// current version of the mod.
	public static void ConfigMigrator() throws Exception {
		FileReader reader = new FileReader(CONFIG_FILE.toFile());
		JsonObject jsonObject = GSON.fromJson(reader, JsonObject.class);

		int version = jsonObject.has("version") ? jsonObject.get("version").getAsInt() : 0;

		if (version < defaultVersion) {
			Constants.LOGGER.warn("Config file is v{}, migrating to v{}!", version, defaultVersion);

			// Add any necessary migrations here based on version
			// For now, no migrations needed

			// Save the config
			byte[] json = GSON.toJson(jsonObject, JsonObject.class).getBytes();
			Files.write(CONFIG_FILE, json, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.CREATE);

			Constants.LOGGER.info("Config file migrated to v{} successfully!", defaultVersion);
		} else if (version > defaultVersion) {
			String message = String.format(
					"Teleport Commands: The config file's version is newer than the supported version, found v%s, expected <= v%s.\n"
							+
							"If you intentionally backported then you can attempt to downgrade the config file located at this location: \"%s\".\n",
					version, defaultVersion, CONFIG_FILE.toAbsolutePath());

			throw new IllegalStateException(message);
		}
	}

	public static void ConfigSaver() throws Exception {
		byte[] json = GSON.toJson(ConfigManager.CONFIG).getBytes();

		Files.write(CONFIG_FILE, json, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.CREATE);
	}

	/// Saves the config after modifications. This method should be called whenever
	/// config values are changed.
	public static void saveConfigChanges() throws Exception {
		ConfigSaver();
		Constants.LOGGER.info("Config changes saved!");
	}

	public static class ConfigClass {
		private final int version = 1;
		public Teleporting teleporting = new Teleporting();
		public Back back = new Back();
		public Home home = new Home();
		public Tpa tpa = new Tpa();
		public Warp warp = new Warp();
		public WorldSpawn worldSpawn = new WorldSpawn();
		public Wild wild = new Wild();
		public Xaero xaero = new Xaero();

		public int getVersion() {
			return version;
		}

		// ===== Convenience Methods =====

		public Teleporting getTeleporting() {
			return teleporting;
		}

		public Back getBack() {
			return back;
		}

		public Home getHome() {
			return home;
		}

		public Tpa getTpa() {
			return tpa;
		}

		public Warp getWarp() {
			return warp;
		}

		public WorldSpawn getWorldSpawn() {
			return worldSpawn;
		}

		public Wild getWild() {
			return wild;
		}

		public Xaero getXaero() {
			return xaero;
		}

		// ===== Configuration Sections =====

		public static final class Teleporting {
			private int delay = 5;
			private int cooldown = 5;

			public int getDelay() {
				return delay;
			}

			public void setDelay(int delay) {
				this.delay = delay;
			}

			public int getCooldown() {
				return cooldown;
			}

			public void setCooldown(int cooldown) {
				this.cooldown = cooldown;
			}
		}

		public final class Back {
			private boolean enabled = true;
			private boolean deleteAfterTeleport = false;

			public boolean isEnabled() {
				return enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public boolean isDeleteAfterTeleport() {
				return deleteAfterTeleport;
			}

			public void setDeleteAfterTeleport(boolean deleteAfterTeleport) {
				this.deleteAfterTeleport = deleteAfterTeleport;
			}
		}

		public final class Home {
			private boolean enabled = true;
			private int playerMaximum = 20;
			private boolean deleteInvalid = false;

			public boolean isEnabled() {
				return enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public int getPlayerMaximum() {
				return playerMaximum;
			}

			public void setPlayerMaximum(int playerMaximum) {
				this.playerMaximum = playerMaximum;
			}

			public boolean isDeleteInvalid() {
				return deleteInvalid;
			}

			public void setDeleteInvalid(boolean deleteInvalid) {
				this.deleteInvalid = deleteInvalid;
			}
		}

		public final class Tpa {
			private boolean enabled = true;
			private int requestExpireTime = 300; // seconds

			public boolean isEnabled() {
				return enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public int getRequestExpireTime() {
				return requestExpireTime;
			}

			public void setRequestExpireTime(int requestExpireTime) {
				this.requestExpireTime = requestExpireTime;
			}
		}

		public final class Warp {
			private boolean enabled = true;
			private int maximum = 0;
			private boolean deleteInvalid = false;

			public boolean isEnabled() {
				return enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public int getMaximum() {
				return maximum;
			}

			public void setMaximum(int maximum) {
				this.maximum = maximum;
			}

			public boolean isDeleteInvalid() {
				return deleteInvalid;
			}

			public void setDeleteInvalid(boolean deleteInvalid) {
				this.deleteInvalid = deleteInvalid;
			}
		}

		public final class Wild {
			private boolean enabled = true;
			private int radius = 500;

			public boolean isEnabled() {
				return enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public int getRadius() {
				return radius;
			}

			public void setRadius(int radius) {
				this.radius = radius;
			}
		}

		public final class WorldSpawn {
			private boolean enabled = true;
			private String world_id = "minecraft:overworld";

			public boolean isEnabled() {
				return enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public String getWorld_id() {
				return world_id;
			}

			public void setWorld_id(String world_id) {
				this.world_id = world_id;
			}
		}

		public final class Xaero {
			private boolean enabled = true;
			private int syncIntervalSeconds = 60;
			private boolean persistWaypointSets = true;
			private String warpSetName = "TeleportCommands Warps";
			private String homeSetName = "TeleportCommands Homes";

			public boolean isEnabled() {
				return enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public int getSyncIntervalSeconds() {
				return syncIntervalSeconds;
			}

			public void setSyncIntervalSeconds(int syncIntervalSeconds) {
				this.syncIntervalSeconds = syncIntervalSeconds;
			}

			public boolean isPersistWaypointSets() {
				return persistWaypointSets;
			}

			public void setPersistWaypointSets(boolean persistWaypointSets) {
				this.persistWaypointSets = persistWaypointSets;
			}

			public String getWarpSetName() {
				return warpSetName;
			}

			public void setWarpSetName(String warpSetName) {
				this.warpSetName = warpSetName;
			}

			public String getHomeSetName() {
				return homeSetName;
			}

			public void setHomeSetName(String homeSetName) {
				this.homeSetName = homeSetName;
			}
		}
	}

	// --- Configuration Management ---
	// The ConfigManager provides a centralized way to manage mod settings:
	//
	// 1. Teleporting: Controls delay, movement/combat restrictions, and cooldowns
	// 2. Back: Manages the /back command behavior
	// 3. Home: Controls home limits, enablement, and invalid location cleanup
	// 4. Tpa: Manages TPA requestability and request expiration time
	// 5. Warp: Controls warp limits, enablement, and invalid location cleanup
	// 6. WorldSpawn: Sets the world and spawn point for /worldspawn
	//
	// To modify configuration values at runtime:
	// ConfigManager.CONFIG.home.setPlayerMaximum(50);
	// ConfigManager.saveConfigChanges();
}
