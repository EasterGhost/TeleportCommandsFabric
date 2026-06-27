package org.AndrewElizabeth.teleportcommandsfabric.modules.worldspawn;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.config.Config;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.TargetTeleportCommandSupport;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.TargetTeleportSafety;
import org.AndrewElizabeth.teleportcommandsfabric.utils.TimeUtils;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class WorldSpawnCommand {
	private static final String ARG_DISABLE_SAFETY = "disableSafety";

	private WorldSpawnCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(buildWorldSpawnNode());
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildWorldSpawnNode() {
		return Commands.literal("worldspawn")
				.requires(source -> source.getPlayer() != null)
				.executes(context -> handleWorldSpawn(context.getSource().getPlayerOrException(), null))
				.then(Commands.argument(ARG_DISABLE_SAFETY, BoolArgumentType.bool())
						.requires(source -> source.getPlayer() != null)
						.executes(context -> handleWorldSpawn(context.getSource().getPlayerOrException(),
								BoolArgumentType.getBool(context, ARG_DISABLE_SAFETY))));
	}

	private static int handleWorldSpawn(ServerPlayer player, Boolean safetyDisabledOverride) {
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

		boolean submitted = TargetTeleportCommandSupport.submit(player, TeleportTarget.centered(world, spawnPos),
				new TargetTeleportCommandSupport.Settings(settings.delaySeconds(), settings.delayTicks(),
						settings.cooldownSeconds(), settings.cooldownMillis(),
						settings.safetyEnabled(safetyDisabledOverride), true),
				"commands.teleport_commands.worldspawn.go", "commands.teleport_commands.common.error",
				"Error while going to the worldspawn.", null,
				(currentPlayer, status, cooldownSeconds, ignored) -> WorldSpawnMessages.sendStatus(currentPlayer, status, cooldownSeconds));
		return submitted ? 0 : 1;
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
				delaySeconds, TimeUtils.secondsToTicks(delaySeconds), cooldownSeconds,
				TimeUtils.secondsToMillis(cooldownSeconds), config.getTeleporting().isDefaultSafetyCheck());
	}

	private record WorldSpawnCommandSettings(boolean enabled, String worldId, int delaySeconds, int delayTicks,
			int cooldownSeconds, long cooldownMillis, boolean defaultSafetyCheck) {
		private boolean safetyEnabled(Boolean safetyDisabledOverride) {
			return TargetTeleportSafety.resolveEnabled(defaultSafetyCheck, safetyDisabledOverride);
		}
	}
}
