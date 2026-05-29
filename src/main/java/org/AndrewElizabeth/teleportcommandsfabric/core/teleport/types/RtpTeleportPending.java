package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public final class RtpTeleportPending implements TeleportOperation {
	private final UUID playerUuid;
	private final long pendingSequence;
	private final long createTick;
	private final int delayTicks;
	private final long cooldownMillis;
	private final boolean recordPrevious;
	private final CompletableFuture<TeleportStatus> resultFuture = new CompletableFuture<>();
	private final BlockPos center;
	private final ResourceKey<Level> dimension;
	private final int minRadius;
	private final int maxRadius;
	private final int maxAttempts;
	private final SplittableRandom random;
	private int remainingAttempts;

	public RtpTeleportPending(UUID playerUuid, long pendingSequence, long createTick, int delayTicks,
			long cooldownMillis, boolean recordPrevious, BlockPos center, ResourceKey<Level> dimension,
			int minRadius, int maxRadius, int maxAttempts) {
		this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
		this.pendingSequence = pendingSequence;
		this.createTick = createTick;
		this.delayTicks = delayTicks;
		this.cooldownMillis = cooldownMillis;
		this.recordPrevious = recordPrevious;
		this.center = Objects.requireNonNull(center, "center").immutable();
		this.dimension = Objects.requireNonNull(dimension, "dimension");
		if (minRadius < 0) {
			throw new IllegalArgumentException("minRadius must be non-negative");
		}
		if (maxRadius < minRadius) {
			throw new IllegalArgumentException("maxRadius must be greater than or equal to minRadius");
		}
		if (maxAttempts < 0) {
			throw new IllegalArgumentException("maxAttempts must be non-negative");
		}
		this.minRadius = minRadius;
		this.maxRadius = maxRadius;
		this.maxAttempts = maxAttempts;
		this.random = new SplittableRandom(ThreadLocalRandom.current().nextLong());
		this.remainingAttempts = maxAttempts;
	}

	@Override
	public UUID playerUuid() { return playerUuid; }
	@Override
	public long pendingSequence() { return pendingSequence; }
	@Override
	public long createTick() { return createTick; }
	@Override
	public int delayTicks() { return delayTicks; }
	@Override
	public long cooldownMillis() { return cooldownMillis; }
	@Override
	public boolean recordPrevious() { return recordPrevious; }
	@Override
	public CompletableFuture<TeleportStatus> resultFuture() { return resultFuture; }

	public BlockPos center() { return center; }
	public ResourceKey<Level> dimension() { return dimension; }
	public int minRadius() { return minRadius; }
	public int maxRadius() { return maxRadius; }
	public int maxAttempts() { return maxAttempts; }
	public int remainingAttempts() { return remainingAttempts; }
	public SplittableRandom random() { return random; }

	public int consumeAttempts(int attempts) {
		if (attempts < 0) {
			throw new IllegalArgumentException("attempts must be non-negative");
		}
		int consumed = Math.min(attempts, remainingAttempts);
		remainingAttempts -= consumed;
		return consumed;
	}

	public boolean isExhausted() {
		return remainingAttempts <= 0;
	}
}
