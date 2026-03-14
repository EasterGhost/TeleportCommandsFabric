package org.AndrewElizabeth.teleportcommandsfabric.commands.back;

import java.util.Optional;
import java.util.UUID;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.common.DeathLocation;
import org.AndrewElizabeth.teleportcommandsfabric.common.PreviousTeleportLocation;
import org.AndrewElizabeth.teleportcommandsfabric.services.TeleportSafety;
import org.AndrewElizabeth.teleportcommandsfabric.services.TeleportService;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.DeathLocationStorage;
import org.AndrewElizabeth.teleportcommandsfabric.storage.PreviousTeleportLocationStorage;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class back {

	public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		commandDispatcher.register(buildBackNode());
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildBackNode() {
		return Commands.literal("back")
				.requires(source -> source.getPlayer() != null)
				.executes(context -> handleBackDeath(context.getSource().getPlayerOrException(), false))
				.then(Commands.argument("Disable Safety", BoolArgumentType.bool())
						.requires(source -> source.getPlayer() != null)
						.executes(context -> handleBackDeath(
								context.getSource().getPlayerOrException(),
								BoolArgumentType.getBool(context, "Disable Safety"))))
				.then(buildBackModeNode("death", back::handleBackDeath))
				.then(buildBackModeNode("tp", back::handleBackTp));
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

		BlockPos teleportBlockPos = resolveTeleportBlockPos(player, deathLocation, deathLocationWorld, safetyDisabled);
		if (teleportBlockPos == null) {
			return 0;
		}

		if (player.blockPosition().equals(teleportBlockPos) && player.level() == deathLocationWorld) {
			BackMessages.sendSame(player);
			return 0;
		}

		Vec3 teleportPos = new Vec3(teleportBlockPos.getX() + 0.5, teleportBlockPos.getY(),
				teleportBlockPos.getZ() + 0.5);

		Runnable onTeleportSuccess = null;
		if (ConfigManager.CONFIG.getBack().isDeleteAfterTeleport()) {
			String playerUuid = player.getStringUUID();
			onTeleportSuccess = () -> DeathLocationStorage.removeDeathLocation(playerUuid);
		}

		if (!TeleportService.teleportWithDelayAndCooldown(player, deathLocationWorld, teleportPos, false,
				onTeleportSuccess)) {
			return 0;
		}

		BackMessages.sendGo(player);
		return 0;
	}

	private static BlockPos resolveTeleportBlockPos(ServerPlayer player, DeathLocation deathLocation,
			ServerLevel deathLocationWorld, boolean safetyDisabled) {
		if (safetyDisabled) {
			return deathLocation.getBlockPos();
		}

		Optional<BlockPos> safeBlockPos = TeleportSafety.getSafeBlockPos(deathLocation.getBlockPos(), deathLocationWorld);
		if (safeBlockPos.isEmpty()) {
			BackMessages.sendUnsafeTeleportPrompt(player, "back death true");
			return null;
		}

		return safeBlockPos.get();
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

		BlockPos teleportBlockPos = resolveTeleportBlockPos(player, previousTeleportLocation, previousTeleportWorld,
				safetyDisabled);
		if (teleportBlockPos == null) {
			return 0;
		}

		if (player.blockPosition().equals(teleportBlockPos) && player.level() == previousTeleportWorld) {
			BackMessages.sendPreviousTeleportSame(player);
			return 0;
		}

		Vec3 teleportPos = new Vec3(teleportBlockPos.getX() + 0.5, teleportBlockPos.getY(),
				teleportBlockPos.getZ() + 0.5);

		if (!TeleportService.teleportWithDelayAndCooldown(player, previousTeleportWorld, teleportPos, false, null,
				false)) {
			return 0;
		}

		BackMessages.sendPreviousTeleportGo(player);
		return 0;
	}

	private static BlockPos resolveTeleportBlockPos(ServerPlayer player,
			PreviousTeleportLocation previousTeleportLocation,
			ServerLevel previousTeleportWorld,
			boolean safetyDisabled) {
		if (safetyDisabled) {
			return previousTeleportLocation.getBlockPos();
		}

		Optional<BlockPos> safeBlockPos = TeleportSafety.getSafeBlockPos(previousTeleportLocation.getBlockPos(),
				previousTeleportWorld);
		if (safeBlockPos.isEmpty()) {
			BackMessages.sendUnsafeTeleportPrompt(player, "back tp true");
			return null;
		}

		return safeBlockPos.get();
	}

	@FunctionalInterface
	private interface BackModeHandler {
		int run(ServerPlayer player, boolean safetyDisabled);
	}
}
