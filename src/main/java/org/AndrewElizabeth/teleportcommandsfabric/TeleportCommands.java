package org.AndrewElizabeth.teleportcommandsfabric;

import com.mojang.brigadier.CommandDispatcher;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportCooldownManager;
import org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.XaeroSyncServer;
import org.AndrewElizabeth.teleportcommandsfabric.modules.admin.AdminCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.back.BackCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.home.HomeCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.rtp.RtpCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.tpa.TpaCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.warp.WarpCommand;
import org.AndrewElizabeth.teleportcommandsfabric.modules.worldspawn.WorldSpawnCommand;
import org.AndrewElizabeth.teleportcommandsfabric.storage.*;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class TeleportCommands implements ModInitializer {
	public static String MOD_LOADER;
	public static Path SAVE_DIR;
	public static Path CONFIG_DIR;
	public static MinecraftServer SERVER;

	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> StorageManager.tick());

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			StorageManager.forceSaveOnShutdown();
			TeleportCommands.SERVER = null;
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			registerCommands(dispatcher);
		});
		MOD_LOADER = "Fabric";
	}

	public static void initializeMod(MinecraftServer server) {
		ModConstants.LOGGER.info("Initializing Teleport Commands (V{})! Hello {}!", ModConstants.VERSION, MOD_LOADER);

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
		AdminCommand.register(dispatcher);
		BackCommand.register(dispatcher);
		HomeCommand.register(dispatcher);
		TpaCommand.register(dispatcher);
		WarpCommand.register(dispatcher);
		WorldSpawnCommand.register(dispatcher);
		RtpCommand.register(dispatcher);
	}

	public static void onPlayerDeath(ServerPlayer player) {
		BlockPos pos = player.blockPosition();
		String world = WorldResolver.getDimensionId(player.level().dimension());
		String uuid = player.getStringUUID();

		DeathLocationStorage.setDeathLocation(uuid, pos, world);
	}
}
