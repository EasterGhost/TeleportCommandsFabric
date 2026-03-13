package org.AndrewElizabeth.teleportcommandsfabric.commands.worldspawn;

import java.util.Objects;
import java.util.Optional;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.services.TeleportSafety;
import org.AndrewElizabeth.teleportcommandsfabric.services.TeleportService;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.world.level.Level.OVERWORLD;

public class worldspawn {

	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		commandDispatcher.register(buildWorldSpawnNode());
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildWorldSpawnNode() {
		return Commands.literal("worldspawn")
				.requires(source -> source.getPlayer() != null)
				.executes(context -> handleWorldSpawn(context.getSource().getPlayerOrException(), false))
				.then(Commands.argument("Disable Safety", BoolArgumentType.bool())
						.requires(source -> source.getPlayer() != null)
						.executes(context -> handleWorldSpawn(
								context.getSource().getPlayerOrException(),
								BoolArgumentType.getBool(context, "Disable Safety"))));
	}

	private static int handleWorldSpawn(ServerPlayer player, boolean safetyDisabled) {
		if (!WorldSpawnMessages.ensureEnabled(player, ConfigManager.CONFIG.getWorldSpawn().isEnabled())) {
			return 1;
		}
		return WorldSpawnMessages.execute(player, "Error while going to the worldspawn! => ",
				() -> toWorldSpawn(player, safetyDisabled));
	}

	private static int toWorldSpawn(ServerPlayer player, boolean safetyDisabled) {
		ServerLevel world = resolveWorld();
		BlockPos worldSpawn = Objects.requireNonNull(world, "World cannot be null!").getLevelData().getRespawnData()
				.pos();

		BlockPos teleportBlockPos = resolveTeleportBlockPos(player, world, worldSpawn, safetyDisabled);
		if (teleportBlockPos == null) {
			return 0;
		}

		if (player.blockPosition().equals(teleportBlockPos) && player.level() == world) {
			WorldSpawnMessages.sendSame(player);
			return 0;
		}

		Vec3 teleportPos = new Vec3(teleportBlockPos.getX() + 0.5, teleportBlockPos.getY(),
				teleportBlockPos.getZ() + 0.5);
		if (TeleportService.teleportWithDelayAndCooldown(player, world, teleportPos, false)) {
			WorldSpawnMessages.sendGo(player);
		}
		return 0;
	}

	private static ServerLevel resolveWorld() {
		String worldId = ConfigManager.CONFIG.getWorldSpawn().getWorld_id();
		ServerLevel world = WorldResolver.getDimensionById(worldId).orElse(null);

		if (world == null) {
			Constants.LOGGER.error("World not found: {}, falling back to overworld", worldId);
			world = TeleportCommands.SERVER.getLevel(OVERWORLD);
		}

		return world;
	}

	private static BlockPos resolveTeleportBlockPos(ServerPlayer player, ServerLevel world, BlockPos worldSpawn,
			boolean safetyDisabled) {
		if (safetyDisabled) {
			return worldSpawn;
		}

		Optional<BlockPos> teleportData = TeleportSafety.getSafeBlockPos(worldSpawn, world);
		if (teleportData.isEmpty()) {
			WorldSpawnMessages.sendUnsafeTeleportPrompt(player);
			return null;
		}

		return teleportData.get();
	}
}
