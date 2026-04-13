package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.WaypointSource;
import org.AndrewElizabeth.teleportcommandsfabric.models.NamedLocation;
import org.AndrewElizabeth.teleportcommandsfabric.models.PlayerData;
import org.AndrewElizabeth.teleportcommandsfabric.utils.WorldResolver;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

final class TemporaryHomeSource extends PlayerHomeSource {
	private static final long TEMP_HOME_TTL_MS = 7L * 24L * 60L * 60L * 1000L;

	TemporaryHomeSource(PlayerData player) {
		super(player);
	}

	@Override
	public Optional<WaypointSource.CreateFailure> validateCreate(ServerPlayer player, String normalizedName) {
		return player().hasTemporaryHome()
				? Optional.of(WaypointSource.CreateFailure.of("commands.teleport_commands.home.tempExists",
						ChatFormatting.RED))
				: Optional.empty();
	}

	@Override
	public NamedLocation createLocation(ServerPlayer player, String normalizedName) {
		long expiredTime = System.currentTimeMillis() + TEMP_HOME_TTL_MS;
		return NamedLocation.createTemporary(normalizedName, player.getBlockX(), player.getY(), player.getBlockZ(),
				WorldResolver.getDimensionId(player.level().dimension()), expiredTime);
	}
}
