package org.AndrewElizabeth.teleportcommandsfabric.modules.back;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportSafety;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportService;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

final class BackCommandSupport {

	private BackCommandSupport() {
	}

	static int teleportResolvedLocation(ServerPlayer player, BlockPos targetBlockPos, ServerLevel targetWorld,
			boolean safetyDisabled, String forceCommand, PlayerMessageSender sameMessage, PlayerMessageSender successMessage,
			Runnable onTeleportSuccess, boolean recordPreviousLocation) {
		BlockPos teleportBlockPos = resolveTeleportBlockPos(
				player, targetBlockPos, targetWorld, safetyDisabled, forceCommand);
		if (teleportBlockPos == null) {
			return 0;
		}

		if (player.blockPosition().equals(teleportBlockPos) && player.level() == targetWorld) {
			sameMessage.send(player);
			return 0;
		}

		Vec3 teleportPos = new Vec3(teleportBlockPos.getX() + 0.5, teleportBlockPos.getY(), teleportBlockPos.getZ() + 0.5);
		if (!TeleportService.teleportWithDelayAndCooldown(player, targetWorld, teleportPos, false, onTeleportSuccess,
				recordPreviousLocation)) {
			return 0;
		}

		successMessage.send(player);
		return 0;
	}

	private static BlockPos resolveTeleportBlockPos(ServerPlayer player, BlockPos targetBlockPos, ServerLevel targetWorld,
			boolean safetyDisabled, String forceCommand) {
		if (safetyDisabled) {
			return targetBlockPos;
		}

		Optional<BlockPos> safeBlockPos = TeleportSafety.getSafeBlockPos(targetBlockPos, targetWorld);
		if (safeBlockPos.isEmpty()) {
			BackMessages.sendUnsafeTeleportPrompt(player, forceCommand);
			return null;
		}

		return safeBlockPos.get();
	}

	@FunctionalInterface
	interface PlayerMessageSender {
		void send(ServerPlayer player);
	}
}
