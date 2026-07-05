package org.AndrewElizabeth.teleportcommandsfabric.modules.worldspawn;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

final class WorldSpawnNodeFactory {
	private static final String ARG_DISABLE_SAFETY = "disableSafety";

	private WorldSpawnNodeFactory() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildWorldSpawnNode() {
		return Commands.literal("worldspawn")
				.requires(source -> source.getPlayer() != null)
				.executes(context -> WorldSpawnTeleportHandler.handleWorldSpawn(
						context.getSource().getPlayerOrException(), null))
				.then(Commands.argument(ARG_DISABLE_SAFETY, BoolArgumentType.bool())
						.requires(source -> source.getPlayer() != null)
						.executes(context -> WorldSpawnTeleportHandler.handleWorldSpawn(
								context.getSource().getPlayerOrException(),
								BoolArgumentType.getBool(context, ARG_DISABLE_SAFETY))));
	}
}
