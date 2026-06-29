package org.AndrewElizabeth.teleportcommandsfabric;

import com.mojang.brigadier.CommandDispatcher;

import org.AndrewElizabeth.teleportcommandsfabric.core.record.AsyncRecordedLocationSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.RtpService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TpaService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportService;
import org.AndrewElizabeth.teleportcommandsfabric.modules.admin.AdminCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.back.BackCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.home.HomeCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.rtp.RtpCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.teleport.TeleportCancelCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.tpa.TpaCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.warp.WarpCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.worldspawn.WorldSpawnCommand;
import org.AndrewElizabeth.teleportcommandsfabric.storage.global.GlobalProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.record.PlayerRecordedLocationManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.player.PlayerProfileManager;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPages;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

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
		ServerLifecycleEvents.SERVER_STARTING.register(TeleportCommands::initializeMod);
		ServerTickEvents.END_SERVER_TICK.register(TeleportCommandsLifecycle::tick);
		ServerLifecycleEvents.SERVER_STOPPING.register(TeleportCommandsLifecycle::shutdown);
		TeleportCommandsLifecycle.registerPlayerConnectionEvents();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
		MOD_LOADER = "Fabric";
	}

	public static void initializeMod(MinecraftServer server) {
		TeleportCommandsLifecycle.initialize(server);
	}

	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		AdminCommand.register(dispatcher);
		BackCommand.register(dispatcher);
		HomeCommand.register(dispatcher);
		TpaCommand.register(dispatcher);
		WarpCommand.register(dispatcher);
		WorldSpawnCommand.register(dispatcher);
		RtpCommand.register(dispatcher);
		TeleportCancelCommand.register(dispatcher);
	}

	public static void onPlayerDeath(ServerPlayer player) {
		try {
			if (RECORDED_LOCATION_SOURCE != null) {
				RECORDED_LOCATION_SOURCE.recordDeathLocation(player.getUUID(), player.blockPosition(), player.level().dimension(),
						player.getYRot(), player.getXRot()).whenComplete((ignored, throwable) -> {
							if (throwable != null) {
								ModConstants.LOGGER.warn("Failed to record death location", throwable);
							}
						});
			}
			if (TELEPORT_SERVICE != null) {
				TELEPORT_SERVICE.onPlayerDeath(player.getUUID());
			}
		} catch (Exception exception) {
			ModConstants.LOGGER.warn("Failed to process player death teleport hooks", exception);
		}
	}
}
