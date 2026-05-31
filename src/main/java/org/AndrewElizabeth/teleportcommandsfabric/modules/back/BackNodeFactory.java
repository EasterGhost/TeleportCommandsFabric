package org.AndrewElizabeth.teleportcommandsfabric.modules.back;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

final class BackNodeFactory {
	private static final String COMMAND_BACK = "back";
	private static final String MODE_DEATH = "death";
	private static final String MODE_TP = "tp";

	private BackNodeFactory() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> buildBackNode() {
		return Commands.literal(COMMAND_BACK)
				.requires(source -> source.getPlayer() != null)
				.executes(context -> BackTeleportHandler.handleBackDeath(context.getSource().getPlayerOrException(), false))
				.then(Commands.argument("Disable Safety", BoolArgumentType.bool())
						.requires(source -> source.getPlayer() != null)
						.executes(context -> BackTeleportHandler.handleBackDeath(context.getSource().getPlayerOrException(),
								BoolArgumentType.getBool(context, "Disable Safety"))))
				.then(buildBackModeNode(MODE_DEATH, BackTeleportHandler::handleBackDeath))
				.then(buildBackModeNode(MODE_TP, BackTeleportHandler::handleBackTp));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildBackModeNode(String mode, BackModeHandler handler) {
		return Commands.literal(mode)
				.requires(source -> source.getPlayer() != null)
				.executes(context -> handler.run(context.getSource().getPlayerOrException(), false))
				.then(Commands.argument("Disable Safety", BoolArgumentType.bool())
						.requires(source -> source.getPlayer() != null)
						.executes(context -> handler.run(context.getSource().getPlayerOrException(),
								BoolArgumentType.getBool(context, "Disable Safety"))));
	}

	@FunctionalInterface
	private interface BackModeHandler {
		int run(ServerPlayer player, boolean safetyDisabled);
	}
}
