package org.AndrewElizabeth.teleportcommandsfabric.modules.worldspawn;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.config.Config;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportService;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.TargetTeleportOptions;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.target.TeleportRequest;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.concurrent.CompletableFuture;

public final class WorldSpawnCommand {
	private static final int TICKS_PER_SECOND = 20;
	private static final long MILLIS_PER_SECOND = 1000L;
	private static final String ARG_DISABLE_SAFETY = "disableSafety";

	private WorldSpawnCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(buildWorldSpawnNode());
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildWorldSpawnNode() {
		return Commands.literal("worldspawn")
				.requires(source -> source.getPlayer() != null)
				.executes(context -> handleWorldSpawn(context.getSource().getPlayerOrException(), false))
				.then(Commands.argument(ARG_DISABLE_SAFETY, BoolArgumentType.bool())
						.requires(source -> source.getPlayer() != null)
						.executes(context -> handleWorldSpawn(context.getSource().getPlayerOrException(),
								BoolArgumentType.getBool(context, ARG_DISABLE_SAFETY))));
	}

	private static int handleWorldSpawn(ServerPlayer player, boolean safetyDisabled) {
		WorldSpawnCommandSettings settings = ConfigManager.query(WorldSpawnCommand::settingsFrom);
		if (!settings.enabled()) {
			WorldSpawnMessages.send(player, "commands.teleport_commands.worldspawn.disabled", ChatFormatting.RED);
			return 1;
		}

		MinecraftServer server = player.level().getServer();
		ServerLevel world = resolveWorld(server, settings.worldId());
		if (world == null) {
			WorldSpawnMessages.send(player, "commands.teleport_commands.common.worldNotFound",
					ChatFormatting.RED, ChatFormatting.BOLD);
			return 1;
		}

		BlockPos spawnPos = world.getRespawnData().pos();
		if (player.level().dimension().equals(world.dimension()) && player.blockPosition().equals(spawnPos)) {
			WorldSpawnMessages.send(player, "commands.teleport_commands.worldspawn.same", ChatFormatting.AQUA);
			return 0;
		}

		TeleportService service = TeleportCommands.TELEPORT_SERVICE;
		if (service == null) {
			WorldSpawnMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED,
					ChatFormatting.BOLD);
			return 1;
		}

		TargetTeleportOptions options = TargetTeleportOptions.builder()
				.delayTicks(settings.delayTicks())
				.cooldownMillis(settings.cooldownMillis())
				.safetyEnabled(!safetyDisabled)
				.recordPrevious(true)
				.build();
		TeleportRequest request = TeleportRequest.resolved(TeleportTarget.centered(world, spawnPos), options);

		try {
			CompletableFuture<TeleportStatus> result = service.request(player, request);
			if (result.isDone()) {
				WorldSpawnMessages.sendStatus(player, result.join(), settings.cooldownSeconds());
				return 0;
			}
			if (settings.delaySeconds() > 0) {
				WorldSpawnMessages.sendDelayStart(player, settings.delaySeconds());
			} else {
				WorldSpawnMessages.send(player, "commands.teleport_commands.worldspawn.go", ChatFormatting.AQUA);
			}
			result.whenComplete((status, throwable) -> server.execute(() -> {
				ServerPlayer currentPlayer = server.getPlayerList().getPlayer(player.getUUID());
				if (currentPlayer == null) {
					return;
				}
				if (throwable != null) {
					ModConstants.LOGGER.error("Error while going to the worldspawn.", throwable);
					WorldSpawnMessages.send(currentPlayer, "commands.teleport_commands.common.error",
							ChatFormatting.RED, ChatFormatting.BOLD);
					return;
				}
				WorldSpawnMessages.sendStatus(currentPlayer, status, settings.cooldownSeconds());
			}));
			return 0;
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Error while going to the worldspawn.", exception);
			WorldSpawnMessages.send(player, "commands.teleport_commands.common.error", ChatFormatting.RED,
					ChatFormatting.BOLD);
			return 1;
		}
	}

	private static ServerLevel resolveWorld(MinecraftServer server, String worldId) {
		ServerLevel world = WorldResolver.getLevelById(server, worldId).orElse(null);
		if (world != null) {
			return world;
		}
		ModConstants.LOGGER.error("World not found: {}, falling back to overworld", worldId);
		return server.getLevel(Level.OVERWORLD);
	}

	private static WorldSpawnCommandSettings settingsFrom(Config config) {
		int delaySeconds = config.getTeleporting().getDelay();
		int cooldownSeconds = config.getTeleporting().getCooldown();
		return new WorldSpawnCommandSettings(config.getWorldSpawn().isEnabled(), config.getWorldSpawn().getWorld_id(),
				delaySeconds, delaySeconds * TICKS_PER_SECOND, cooldownSeconds,
				cooldownSeconds * MILLIS_PER_SECOND);
	}

	private record WorldSpawnCommandSettings(boolean enabled, String worldId, int delaySeconds, int delayTicks,
			int cooldownSeconds, long cooldownMillis) {
	}
}
