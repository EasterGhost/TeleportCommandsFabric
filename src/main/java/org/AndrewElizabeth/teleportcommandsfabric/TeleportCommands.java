package org.AndrewElizabeth.teleportcommandsfabric;

import com.mojang.brigadier.CommandDispatcher;

import org.AndrewElizabeth.teleportcommandsfabric.commands.admin.AdminCommands;
import org.AndrewElizabeth.teleportcommandsfabric.commands.back.back;
import org.AndrewElizabeth.teleportcommandsfabric.commands.home.home;
import org.AndrewElizabeth.teleportcommandsfabric.commands.rtp.rtp;
import org.AndrewElizabeth.teleportcommandsfabric.commands.tpa.tpa;
import org.AndrewElizabeth.teleportcommandsfabric.commands.warp.warp;
import org.AndrewElizabeth.teleportcommandsfabric.commands.worldspawn.worldspawn;
import org.AndrewElizabeth.teleportcommandsfabric.storage.*;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;
import org.AndrewElizabeth.teleportcommandsfabric.xaero.XaeroSyncServer;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.core.BlockPos;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class TeleportCommands implements ModInitializer {
	public static String MOD_LOADER;
	public static Path SAVE_DIR;
	public static Path CONFIG_DIR;
	public static MinecraftServer SERVER;

	@Override
	public void onInitialize() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK
				.register(server -> org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager.tick());

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager.forceSaveOnShutdown();
		});
		MOD_LOADER = "Fabric";
	}

	public static void initializeMod(MinecraftServer server) {
		Constants.LOGGER.info("Initializing Teleport Commands (V{})! Hello {}!", Constants.VERSION, MOD_LOADER);

		SAVE_DIR = Path.of(String.valueOf(server.getWorldPath(LevelResource.ROOT)));
		CONFIG_DIR = Paths.get(System.getProperty("user.dir")).resolve("config");
		SERVER = server;
		ConfigManager.ConfigInit();
		StorageManager.StorageInit();
		DeathLocationStorage.clearDeathLocations();
		PreviousTeleportLocationStorage.clearPreviousTeleportLocations();
		TeleportCooldownManager.clearAll();
		XaeroSyncServer.initialize();
		ServerPlayConnectionEvents.DISCONNECT.register((handler, s) -> {
			UUID playerUuid = handler.player.getUUID();
			LogoutCacheManager.scheduleCleanup(playerUuid);
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, s) -> {
			UUID playerUuid = handler.player.getUUID();
			LogoutCacheManager.cancelCleanup(playerUuid);
		});
	}

	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		AdminCommands.register(dispatcher);
		back.register(dispatcher);
		home.register(dispatcher);
		tpa.register(dispatcher);
		warp.register(dispatcher);
		worldspawn.register(dispatcher);
		rtp.register(dispatcher);
	}

	public static void onPlayerDeath(ServerPlayer player) {
		BlockPos pos = new BlockPos(player.getBlockX(), player.getBlockY(), player.getBlockZ());
		String world = WorldResolver.getDimensionId(player.level().dimension());
		String uuid = player.getStringUUID();

		DeathLocationStorage.setDeathLocation(uuid, pos, world);
	}
}
