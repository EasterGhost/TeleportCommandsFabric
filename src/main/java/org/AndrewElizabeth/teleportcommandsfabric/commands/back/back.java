package org.AndrewElizabeth.teleportcommandsfabric.commands.back;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.common.DeathLocation;
import org.AndrewElizabeth.teleportcommandsfabric.common.PreviousTeleportLocation;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.DeathLocationStorage;
import org.AndrewElizabeth.teleportcommandsfabric.storage.PreviousTeleportLocationStorage;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class back {
	private static final String COMMAND_BACK = "back";
	private static final String MODE_DEATH = "death";
	private static final String MODE_TP = "tp";
	private static final String COMMAND_BACK_DEATH_FORCE = COMMAND_BACK + " " + MODE_DEATH + " true";
	private static final String COMMAND_BACK_TP_FORCE = COMMAND_BACK + " " + MODE_TP + " true";

	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		commandDispatcher.register(buildBackNode());
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildBackNode() {
		return Commands.literal(COMMAND_BACK)
				.requires(source -> source.getPlayer() != null)
				.executes(context -> handleBackDeath(context.getSource().getPlayerOrException(), false))
				.then(Commands.argument("Disable Safety", BoolArgumentType.bool())
						.requires(source -> source.getPlayer() != null)
						.executes(context -> handleBackDeath(
								context.getSource().getPlayerOrException(),
								BoolArgumentType.getBool(context, "Disable Safety"))))
				.then(buildBackModeNode(MODE_DEATH, back::handleBackDeath))
				.then(buildBackModeNode(MODE_TP, back::handleBackTp));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildBackModeNode(
			String mode,
			BackModeHandler handler) {
		return Commands.literal(mode)
				.requires(source -> source.getPlayer() != null)
				.executes(context -> handler.run(context.getSource().getPlayerOrException(), false))
				.then(Commands.argument("Disable Safety", BoolArgumentType.bool())
						.requires(source -> source.getPlayer() != null)
						.executes(context -> handler.run(
								context.getSource().getPlayerOrException(),
								BoolArgumentType.getBool(context, "Disable Safety"))));
	}

	private static int handleBackDeath(ServerPlayer player, boolean safetyDisabled) {
		if (!BackMessages.ensureEnabled(player, ConfigManager.CONFIG.getBack().isEnabled())) {
			return 1;
		}
		return BackMessages.execute(player, "Error while going back! => ",
				() -> toDeathLocation(player, safetyDisabled));
	}

	private static int handleBackTp(ServerPlayer player, boolean safetyDisabled) {
		if (!BackMessages.ensureEnabled(player, ConfigManager.CONFIG.getBack().isEnabled())) {
			return 1;
		}
		return BackMessages.execute(player, "Error while going back to previous teleport location! => ",
				() -> toPreviousTeleportLocation(player, safetyDisabled));
	}

	private static int toDeathLocation(ServerPlayer player, boolean safetyDisabled) throws Exception {
		DeathLocation deathLocation = DeathLocationStorage
				.getDeathLocation(player.getStringUUID())
				.orElse(null);

		if (deathLocation == null) {
			BackMessages.sendNoLocation(player);
			if (PreviousTeleportLocationStorage.getPreviousTeleportLocation(player.getUUID()).isPresent()) {
				BackMessages.sendTryBackTpPrompt(player);
			}
			return 0;
		}

		ServerLevel deathLocationWorld = deathLocation.getWorld().orElse(null);
		if (deathLocationWorld == null) {
			Constants.LOGGER.warn(
					"({}) Error while going back! \nCouldn't find a world with the id: \"{}\"",
					player.getName().getString(),
					deathLocation.getWorldString());
			BackMessages.sendWorldNotFound(player);
			return 0;
		}

		Runnable onTeleportSuccess = null;
		if (ConfigManager.CONFIG.getBack().isDeleteAfterTeleport()) {
			String playerUuid = player.getStringUUID();
			onTeleportSuccess = () -> DeathLocationStorage.removeDeathLocation(playerUuid);
		}

		return BackCommandSupport.teleportResolvedLocation(
				player,
				deathLocation.getBlockPos(),
				deathLocationWorld,
				safetyDisabled,
				COMMAND_BACK_DEATH_FORCE,
				BackMessages::sendSame,
				BackMessages::sendGo,
				onTeleportSuccess,
				true);
	}

	private static int toPreviousTeleportLocation(ServerPlayer player, boolean safetyDisabled) throws Exception {
		UUID playerUuid = player.getUUID();
		PreviousTeleportLocation previousTeleportLocation = PreviousTeleportLocationStorage
				.getPreviousTeleportLocation(playerUuid)
				.orElse(null);

		if (previousTeleportLocation == null) {
			BackMessages.sendNoPreviousTeleportLocation(player);
			return 0;
		}

		ServerLevel previousTeleportWorld = previousTeleportLocation.getWorld().orElse(null);
		if (previousTeleportWorld == null) {
			Constants.LOGGER.warn(
					"({}) Error while going back to previous teleport location! \nCouldn't find a world with the id: \"{}\"",
					player.getName().getString(),
					previousTeleportLocation.getWorldString());
			BackMessages.sendWorldNotFound(player);
			return 0;
		}

		return BackCommandSupport.teleportResolvedLocation(
				player,
				previousTeleportLocation.getBlockPos(),
				previousTeleportWorld,
				safetyDisabled,
				COMMAND_BACK_TP_FORCE,
				BackMessages::sendPreviousTeleportSame,
				BackMessages::sendPreviousTeleportGo,
				null,
				false);
	}

	@FunctionalInterface
	private interface BackModeHandler {
		int run(ServerPlayer player, boolean safetyDisabled);
	}
}
