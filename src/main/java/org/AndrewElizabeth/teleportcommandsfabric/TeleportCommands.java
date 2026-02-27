package org.AndrewElizabeth.teleportcommandsfabric;

import com.mojang.brigadier.CommandDispatcher;
import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager;
import org.AndrewElizabeth.teleportcommandsfabric.commands.*;
import org.AndrewElizabeth.teleportcommandsfabric.storage.DeathLocationStorage;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.TeleportCooldownManager;
import org.AndrewElizabeth.teleportcommandsfabric.xaero.XaeroSyncServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.core.BlockPos;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.fabricmc.api.ModInitializer;

public class TeleportCommands implements ModInitializer {
	public static String MOD_LOADER;
	public static Path SAVE_DIR;
	public static Path CONFIG_DIR;
	public static MinecraftServer SERVER;

	@Override
	public void onInitialize() {
		MOD_LOADER = "Fabric";
	}

	// Gets ran when the server starts, initializes the mod :3
	public static void initializeMod(MinecraftServer server) {
		Constants.LOGGER.info("Initializing Teleport Commands (V{})! Hello {}!", Constants.VERSION, MOD_LOADER);

		SAVE_DIR = Path.of(String.valueOf(server.getWorldPath(LevelResource.ROOT)));
		CONFIG_DIR = Paths.get(System.getProperty("user.dir")).resolve("config"); // Construct the game directory path
		SERVER = server;
		ConfigManager.ConfigInit(); // Load config before anything depends on it
		StorageManager.StorageInit(); // Initialize the storage file
		DeathLocationStorage.clearDeathLocations(); // Clear data of death locations.
		TeleportCooldownManager.clearAll(); // Clear teleport cooldowns and scheduled teleports
		XaeroSyncServer.initialize(); // Register Xaero sync handlers
	}

	// initialize commands, also allows me to easily disable any when there is a
	// config
	public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		back.register(dispatcher);
		home.register(dispatcher);
		tpa.register(dispatcher);
		warp.register(dispatcher);
		worldspawn.register(dispatcher);
		rtp.register(dispatcher);
		main.register(dispatcher);
	}

	// Runs when the playerDeath mixin calls it, updates the /back command position
	public static void onPlayerDeath(ServerPlayer player) {
		BlockPos pos = new BlockPos(player.getBlockX(), player.getBlockY(), player.getBlockZ());
		String world = player.level().dimension().toString();
		String uuid = player.getStringUUID();

		DeathLocationStorage.setDeathLocation(uuid, pos, world);
	}
}
