package org.AndrewElizabeth.teleportcommandsfabric;

import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.record.PlayerRecordedLocationSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.RtpService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TpaService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportOperationManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportPreloadManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.SafetyThreadPool;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportBatchDispatcher;
import org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.XaeroSyncServer;
import org.AndrewElizabeth.teleportcommandsfabric.modules.tpa.TpaCommand;
import org.AndrewElizabeth.teleportcommandsfabric.storage.LegacyStorageMigrator;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.record.PlayerRecordedLocationManager;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPages;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class TeleportCommandsLifecycle {
	private static boolean playerConnectionEventsRegistered;

	private TeleportCommandsLifecycle() {
	}

	static void initialize(MinecraftServer server) {
		ModConstants.LOGGER.info("Initializing Teleport Commands (V{})! Hello {}!", ModConstants.VERSION,
				TeleportCommands.MOD_LOADER);

		TeleportCommands.SAVE_DIR = Path.of(String.valueOf(server.getWorldPath(LevelResource.ROOT)));
		TeleportCommands.CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
		TeleportCommands.SERVER = server;
		runLegacyStorageMigration();
		initializeStorageManagers();
		ConfigManager.initialize();
		loadStorageManagers();
		XaeroSyncServer.initialize();
	}

	static void tick(MinecraftServer server) {
		if (TeleportCommands.TELEPORT_SERVICE != null) {
			TeleportCommands.TELEPORT_SERVICE.tick(server);
		}
		if (TeleportCommands.TPA_SERVICE != null) {
			TeleportCommands.TPA_SERVICE.tick(server);
		}
		if (TeleportCommands.RTP_SERVICE != null) {
			TeleportCommands.RTP_SERVICE.tick(server);
		}
	}

	static void shutdown(MinecraftServer server) {
		shutdownStorageManagers();
		TeleportCommands.SERVER = null;
	}

	private static void initializeStorageManagers() {
		TeleportCommands.RECORDED_LOCATION_MANAGER = new PlayerRecordedLocationManager();
		TeleportCommands.RECORDED_LOCATION_SOURCE = new PlayerRecordedLocationSource(
				TeleportCommands.RECORDED_LOCATION_MANAGER);
		TeleportOperationManager teleportOperationManager = new TeleportOperationManager();
		TeleportPreloadManager teleportPreloadManager = new TeleportPreloadManager();
		TeleportCommands.TELEPORT_SERVICE = new TeleportService(TeleportCommands.RECORDED_LOCATION_SOURCE,
				teleportOperationManager, teleportPreloadManager, new TeleportBatchDispatcher(), new SafetyThreadPool());
		TeleportCommands.TPA_SERVICE = new TpaService(TeleportCommands.RECORDED_LOCATION_SOURCE,
				teleportOperationManager, teleportPreloadManager, TpaCommand::sendExpired);
		TeleportCommands.RTP_SERVICE = new RtpService(TeleportCommands.RECORDED_LOCATION_SOURCE,
				teleportOperationManager, teleportPreloadManager);
		TeleportCommands.GLOBAL_PROFILE_MANAGER = new GlobalProfileManager();
		TeleportCommands.PLAYER_PROFILE_MANAGER = new PlayerProfileManager();
		TeleportCommands.WAYPOINT_PAGES = new WaypointPages();
	}

	private static void runLegacyStorageMigration() {
		try {
			LegacyStorageMigrator.migrateIfPresent();
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to migrate legacy storage.json", exception);
		}
	}

	private static void loadStorageManagers() {
		CompletableFuture<?> globalLoad = TeleportCommands.GLOBAL_PROFILE_MANAGER.load();
		CompletableFuture<Void> recordLoad = CompletableFuture
				.runAsync(() -> TeleportCommands.RECORDED_LOCATION_MANAGER.loadRecords());
		CompletableFuture.allOf(globalLoad, recordLoad).join();
	}

	static synchronized void registerPlayerConnectionEvents() {
		if (playerConnectionEventsRegistered) {
			return;
		}
		playerConnectionEventsRegistered = true;

		ServerPlayConnectionEvents.DISCONNECT.register((handler, s) -> {
			UUID playerUuid = handler.player.getUUID();
			if (TeleportCommands.PLAYER_PROFILE_MANAGER != null) {
				TeleportCommands.PLAYER_PROFILE_MANAGER.onPlayerQuit(playerUuid);
			}
			if (TeleportCommands.TELEPORT_SERVICE != null) {
				TeleportCommands.TELEPORT_SERVICE.onPlayerQuit(playerUuid);
			}
			if (TeleportCommands.TPA_SERVICE != null) {
				TeleportCommands.TPA_SERVICE.onPlayerQuit(playerUuid);
			}
			if (TeleportCommands.RTP_SERVICE != null) {
				TeleportCommands.RTP_SERVICE.onPlayerQuit(playerUuid);
			}
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, s) -> {
			UUID playerUuid = handler.player.getUUID();
			if (TeleportCommands.PLAYER_PROFILE_MANAGER != null) {
				TeleportCommands.PLAYER_PROFILE_MANAGER.onPlayerJoin(playerUuid);
			}
			if (TeleportCommands.TELEPORT_SERVICE != null) {
				TeleportCommands.TELEPORT_SERVICE.onPlayerJoin(playerUuid);
			}
		});
	}

	private static void shutdownStorageManagers() {
		TeleportService teleportService = TeleportCommands.TELEPORT_SERVICE;
		TpaService tpaService = TeleportCommands.TPA_SERVICE;
		RtpService rtpService = TeleportCommands.RTP_SERVICE;
		WaypointPages waypointPages = TeleportCommands.WAYPOINT_PAGES;
		PlayerRecordedLocationManager recordedLocationManager = TeleportCommands.RECORDED_LOCATION_MANAGER;
		GlobalProfileManager globalProfileManager = TeleportCommands.GLOBAL_PROFILE_MANAGER;
		PlayerProfileManager playerProfileManager = TeleportCommands.PLAYER_PROFILE_MANAGER;

		if (teleportService != null) {
			teleportService.shutdown();
		}
		if (tpaService != null) {
			tpaService.shutdown();
		}
		if (rtpService != null) {
			rtpService.shutdown();
		}
		if (waypointPages != null) {
			waypointPages.close();
		}
		TeleportCommands.TELEPORT_SERVICE = null;
		TeleportCommands.TPA_SERVICE = null;
		TeleportCommands.RTP_SERVICE = null;
		TeleportCommands.WAYPOINT_PAGES = null;

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
			TeleportCommands.RECORDED_LOCATION_MANAGER = null;
			TeleportCommands.RECORDED_LOCATION_SOURCE = null;
		}
		if (globalProfileManager != null) {
			TeleportCommands.GLOBAL_PROFILE_MANAGER = null;
		}
		if (playerProfileManager != null) {
			TeleportCommands.PLAYER_PROFILE_MANAGER = null;
		}
	}
}
