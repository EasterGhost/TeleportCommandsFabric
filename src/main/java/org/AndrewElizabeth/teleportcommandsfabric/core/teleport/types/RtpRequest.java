package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.UUID;

public record RtpRequest(UUID playerUuid, BlockPos center, ResourceKey<Level> dimension, int minRadius,
		int maxRadius, int maxAttempts, int delayTicks, long cooldownMillis, boolean recordPrevious) {
	public RtpRequest {
		Objects.requireNonNull(playerUuid, "playerUuid");
		center = Objects.requireNonNull(center, "center").immutable();
		Objects.requireNonNull(dimension, "dimension");
		if (minRadius < 0) {
			throw new IllegalArgumentException("minRadius must be non-negative");
		}
		if (maxRadius < minRadius) {
			throw new IllegalArgumentException("maxRadius must be greater than or equal to minRadius");
		}
		if (maxAttempts < 0) {
			throw new IllegalArgumentException("maxAttempts must be non-negative");
		}
		if (delayTicks < 0) {
			throw new IllegalArgumentException("delayTicks must be non-negative");
		}
		if (cooldownMillis < 0L) {
			throw new IllegalArgumentException("cooldownMillis must be non-negative");
		}
	}
}
