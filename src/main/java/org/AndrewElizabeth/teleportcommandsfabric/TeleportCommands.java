package org.AndrewElizabeth.teleportcommandsfabric;

import com.mojang.brigadier.CommandDispatcher;

import org.AndrewElizabeth.teleportcommandsfabric.core.record.AsyncRecordedLocationSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.record.PlayerRecordedLocationSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.RtpService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TpaService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportOperationManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportPreloadManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.SafetyThreadPool;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportBatchDispatcher;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportService;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.XaeroSyncServer;
import org.AndrewElizabeth.teleportcommandsfabric.modules.admin.AdminCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.back.BackCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.home.HomeCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.rtp.RtpCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.tpa.TpaCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.warp.WarpCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.worldspawn.WorldSpawnCommand;
import org.AndrewElizabeth.teleportcommandsfabric.storage.LegacyStorageMigrator;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.record.PlayerRecordedLocationManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPages;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

public class TeleportCommands implements ModInitializer {
	public static String MOD_LOADER;
	public static Path SAVE_DIR;
	public static Path CONFIG_DIR;
	public static MinecraftServer SERVER;
	public static PlayerProfileManager PLAYER_PROFILE_MANAGER;
	public static GlobalProfileManager GLOBAL_PROFILE_MANAGER;
	public static PlayerRecordedLocationManager RECORDED_LOCATION_MANAGER;
	public static AsyncRecordedLocationSource RECORDED_LOCATION_SOURCE;
	public static TeleportService TELEPORT_SERVICE;
	public static TpaService TPA_SERVICE;
	public static RtpService RTP_SERVICE;
	public static WaypointPages WAYPOINT_PAGES;

	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (TELEPORT_SERVICE != null) {
				TELEPORT_SERVICE.tick(server);
			}
			if (TPA_SERVICE != null) {
				TPA_SERVICE.tick(server);
			}
			if (RTP_SERVICE != null) {
				RTP_SERVICE.tick(server);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			shutdownStorageManagers();
			SERVER = null;
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
		MOD_LOADER = "Fabric";
	}

	public static void initializeMod(MinecraftServer server) {
		ModConstants.LOGGER.info("Initializing Teleport Commands (V{})! Hello {}!", ModConstants.VERSION, MOD_LOADER);

		SAVE_DIR = Path.of(String.valueOf(server.getWorldPath(LevelResource.ROOT)));
		CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
		SERVER = server;
		runLegacyStorageMigration();
		initializeStorageManagers();
		ConfigManager.initialize();
		loadStorageManagers();
		XaeroSyncServer.initialize();
		registerPlayerConnectionEvents();
	}

	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		AdminCommand.register(dispatcher);
		BackCommand.register(dispatcher);
		HomeCommand.register(dispatcher);
		TpaCommand.register(dispatcher);
		WarpCommand.register(dispatcher);
		WorldSpawnCommand.register(dispatcher);
		RtpCommand.register(dispatcher);
	}

	public static void onPlayerDeath(ServerPlayer player) {
		if (RECORDED_LOCATION_SOURCE != null) {
			RECORDED_LOCATION_SOURCE.recordDeathLocation(player.getUUID(), player.blockPosition(), player.level().dimension())
					.whenComplete((ignored, throwable) -> {
						if (throwable != null) {
							ModConstants.LOGGER.warn("Failed to record death location", throwable);
						}
					});
		}
		if (TELEPORT_SERVICE != null) {
			TELEPORT_SERVICE.onPlayerDeath(player.getUUID());
		}
	}

	private static void initializeStorageManagers() {
		RECORDED_LOCATION_MANAGER = new PlayerRecordedLocationManager();
		RECORDED_LOCATION_SOURCE = new PlayerRecordedLocationSource(RECORDED_LOCATION_MANAGER);
		TeleportOperationManager teleportOperationManager = new TeleportOperationManager();
		TeleportPreloadManager teleportPreloadManager = new TeleportPreloadManager();
		TELEPORT_SERVICE = new TeleportService(RECORDED_LOCATION_SOURCE, teleportOperationManager, teleportPreloadManager,
				new TeleportBatchDispatcher(), new SafetyThreadPool());
		TPA_SERVICE = new TpaService(RECORDED_LOCATION_SOURCE, teleportOperationManager, teleportPreloadManager,
				TpaCommand::sendExpired);
		RTP_SERVICE = new RtpService(RECORDED_LOCATION_SOURCE, teleportOperationManager, teleportPreloadManager);
		GLOBAL_PROFILE_MANAGER = new GlobalProfileManager();
		PLAYER_PROFILE_MANAGER = new PlayerProfileManager();
		WAYPOINT_PAGES = new WaypointPages();
	}

	private static void runLegacyStorageMigration() {
		try {
			LegacyStorageMigrator.migrateIfPresent();
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to migrate legacy storage.json", exception);
		}
	}

	private static void loadStorageManagers() {
		CompletableFuture<?> globalLoad = GLOBAL_PROFILE_MANAGER.load();
		CompletableFuture<Void> recordLoad = CompletableFuture.runAsync(() -> RECORDED_LOCATION_MANAGER.loadRecords());
		CompletableFuture.allOf(globalLoad, recordLoad).join();
	}

	private static void registerPlayerConnectionEvents() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, s) -> {
			UUID playerUuid = handler.player.getUUID();
			if (PLAYER_PROFILE_MANAGER != null) {
				PLAYER_PROFILE_MANAGER.onPlayerQuit(playerUuid);
			}
			if (TELEPORT_SERVICE != null) {
				TELEPORT_SERVICE.onPlayerQuit(playerUuid);
			}
			if (TPA_SERVICE != null) {
				TPA_SERVICE.onPlayerQuit(playerUuid);
			}
			if (RTP_SERVICE != null) {
				RTP_SERVICE.onPlayerQuit(playerUuid);
			}
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, s) -> {
			UUID playerUuid = handler.player.getUUID();
			if (PLAYER_PROFILE_MANAGER != null) {
				PLAYER_PROFILE_MANAGER.onPlayerJoin(playerUuid);
			}
			if (TELEPORT_SERVICE != null) {
				TELEPORT_SERVICE.onPlayerJoin(playerUuid);
			}
		});
	}

	private static void shutdownStorageManagers() {
		TeleportService teleportService = TELEPORT_SERVICE;
		RtpService rtpService = RTP_SERVICE;
		WaypointPages waypointPages = WAYPOINT_PAGES;
		PlayerRecordedLocationManager recordedLocationManager = RECORDED_LOCATION_MANAGER;
		GlobalProfileManager globalProfileManager = GLOBAL_PROFILE_MANAGER;
		PlayerProfileManager playerProfileManager = PLAYER_PROFILE_MANAGER;

		if (teleportService != null) {
			teleportService.shutdown();
		}
		if (TPA_SERVICE != null) {
			TPA_SERVICE.shutdown();
		}
		if (rtpService != null) {
			rtpService.shutdown();
		}
		if (waypointPages != null) {
			waypointPages.close();
		}
		TELEPORT_SERVICE = null;
		TPA_SERVICE = null;
		RTP_SERVICE = null;
		WAYPOINT_PAGES = null;

		CompletableFuture<Void> configShutdown = ConfigManager.shutdown();
		CompletableFuture<Void> recordSave = recordedLocationManager == null
				? CompletableFuture.completedFuture(null)
				: CompletableFuture.runAsync(recordedLocationManager::saveRecords);
		CompletableFuture<Void> globalShutdown = globalProfileManager == null
				? CompletableFuture.completedFuture(null)
				: globalProfileManager.shutdown();
		CompletableFuture<Void> playerShutdown = playerProfileManager == null
				? CompletableFuture.completedFuture(null)
				: playerProfileManager.shutdown();

		CompletableFuture.allOf(configShutdown, recordSave, globalShutdown, playerShutdown).join();

		if (recordedLocationManager != null) {
			recordedLocationManager.clear();
			RECORDED_LOCATION_MANAGER = null;
			RECORDED_LOCATION_SOURCE = null;
		}
		if (globalProfileManager != null) {
			GLOBAL_PROFILE_MANAGER = null;
		}
		if (playerProfileManager != null) {
			PLAYER_PROFILE_MANAGER = null;
		}
	}
}
