package org.AndrewElizabeth.teleportcommandsfabric.core.teleport;

import org.AndrewElizabeth.teleportcommandsfabric.models.NamedLocation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class NamedLocationTeleportService {
	private NamedLocationTeleportService() {
	}

	public static Optional<ServerLevel> resolveWorld(NamedLocation location) {
		return location.getWorld();
	}

	public static boolean isAlreadyAtDestination(ServerPlayer player, ServerLevel world, NamedLocation location) {
		BlockPos teleportBlockPos = location.getBlockPos();
		return player.blockPosition().equals(teleportBlockPos) && player.level() == world;
	}

	public static boolean teleportToNamedLocation(ServerPlayer player, ServerLevel world, NamedLocation location) {
		BlockPos teleportBlockPos = location.getBlockPos();
		Vec3 teleportPos = new Vec3(teleportBlockPos.getX() + 0.5, location.getYPrecise(), teleportBlockPos.getZ() + 0.5);
		return TeleportService.teleportWithDelayAndCooldown(player, world, teleportPos, false);
	}
}
