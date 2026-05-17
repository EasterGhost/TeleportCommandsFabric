package org.AndrewElizabeth.teleportcommandsfabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

public class ConfigManager {
	private static Path CONFIG_FILE;
	private static Config CONFIG;
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(Duration.class, new TypeAdapter<Duration>() {
				@Override
				public void write(JsonWriter out, Duration value) throws IOException {
					if (value == null) {
						out.nullValue();
					} else {
						out.value(value.getSeconds());
					}
				}

				@Override
				public Duration read(JsonReader in) throws IOException {
					if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
						in.nextNull();
						return null;
					}
					return Duration.ofSeconds(in.nextLong());
				}
			})
			.create();
	private static ExecutorService IO_EXECUTOR;
	private static final Object CONFIG_LOCK = new Object();

	public static void initialize() {
		CONFIG_FILE = TeleportCommands.CONFIG_DIR.resolve("teleport_commands.json");

		try {
			reload().join();
		} catch (Exception e) {
			ModConstants.LOGGER.error("Error while initializing the config file! Exiting! => ", e);
			throw new RuntimeException("Error while initializing the config file! Exiting! => ", e);
		}
	}

	public static CompletableFuture<Void> reload() {
		return CompletableFuture.runAsync(() -> {
			try {
				loadConfig();
				saveConfigSync();
				ConfigApplier.applyStorage();
			} catch (Exception exception) {
				throw new CompletionException(exception);
			}
		}, ioExecutor());
	}

	private static void loadConfig() throws Exception {
		if (!CONFIG_FILE.toFile().exists() || CONFIG_FILE.toFile().length() == 0) {
			Files.createDirectories(TeleportCommands.CONFIG_DIR);

			ModConstants.LOGGER.warn("Config file was not found or was empty! Initializing config");
			setConfig(new Config());
			ModConstants.LOGGER.info("Config created successfully!");
			return;
		}

		ConfigMigrator.migrate(CONFIG_FILE, GSON, ModConstants.CONFIG_VERSION);

		Config loadedConfig;
		try (BufferedReader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
			loadedConfig = GSON.fromJson(reader, Config.class);
		}
		if (loadedConfig == null) {
			ModConstants.LOGGER.warn("Config file was empty! Loading defaults...");
			setConfig(new Config());
			return;
		}

		setConfig(loadedConfig);
		ModConstants.LOGGER.info("Config loaded successfully!");
	}

	private static void saveConfigSync() {
		byte[] json = serializeConfig();
		if (json == null)
			return;

		try {
			Path tempFile = TeleportCommands.CONFIG_DIR.resolve("teleport_commands.json.tmp");
			Files.write(tempFile, json, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.CREATE);
			Files.move(tempFile, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			ModConstants.LOGGER.error("Error while saving the config file! => ", e);
		}
	}

	private static void saveConfigAsync() {
		ioExecutor().submit(() -> {
			byte[] json = serializeConfig();
			if (json == null)
				return;

			try {
				Path tempFile = TeleportCommands.CONFIG_DIR.resolve("teleport_commands.json.tmp");
				Files.write(tempFile, json, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
						StandardOpenOption.CREATE);
				Files.move(tempFile, CONFIG_FILE, StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception e) {
				ModConstants.LOGGER.error("Error while saving the config file asynchronously! => ", e);
			}
		});
	}

	public static CompletableFuture<Void> shutdown() {
		ExecutorService executor = ioExecutor();
		return CompletableFuture.runAsync(ConfigManager::saveConfigSync, executor)
				.whenComplete((ignored, throwable) -> executor.shutdown());
	}

	public static <T> T query(Function<Config, T> reader) {
		synchronized (CONFIG_LOCK) {
			return reader.apply(requireConfig());
		}
	}

	public static void mutate(Consumer<Config> writer) {
		synchronized (CONFIG_LOCK) {
			Config config = requireConfig();
			writer.accept(config);
			config.normalize();
		}
		saveConfigAsync();
		ConfigApplier.applyStorage();
	}

	private static void setConfig(Config config) {
		synchronized (CONFIG_LOCK) {
			CONFIG = config.normalize();
		}
	}

	private static Config requireConfig() {
		if (CONFIG == null) {
			throw new IllegalStateException("Config has not been initialized.");
		}
		return CONFIG;
	}

	private static byte[] serializeConfig() {
		synchronized (CONFIG_LOCK) {
			if (CONFIG_FILE == null || CONFIG == null) {
				ModConstants.LOGGER.error("Cannot save config: CONFIG_FILE or CONFIG is null.");
				return null;
			}
			return GSON.toJson(CONFIG).getBytes(StandardCharsets.UTF_8);
		}
	}

	private static synchronized ExecutorService ioExecutor() {
		if (IO_EXECUTOR == null || IO_EXECUTOR.isShutdown()) {
			IO_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
				Thread thread = new Thread(runnable, "tpc-config-io");
				thread.setDaemon(true);
				return thread;
			});
		}
		return IO_EXECUTOR;
	}
}
