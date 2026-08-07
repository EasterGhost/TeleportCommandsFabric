package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.wild;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportOperation;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public final class WildTeleportPending implements TeleportOperation {
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
	private final SplittableRandom random;

	public WildTeleportPending(UUID playerUuid, long pendingSequence, long createTick, WildRequest request,
			BlockPos center, ResourceKey<Level> dimension) {
		this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
		this.pendingSequence = pendingSequence;
		this.createTick = createTick;
		WildRequest safeRequest = Objects.requireNonNull(request, "request");
		this.delayTicks = safeRequest.delayTicks();
		this.cooldownMillis = safeRequest.cooldownMillis();
		this.recordPrevious = safeRequest.recordPrevious();
		this.center = Objects.requireNonNull(center, "center").immutable();
		this.dimension = Objects.requireNonNull(dimension, "dimension");
		this.minRadius = safeRequest.minRadius();
		this.maxRadius = safeRequest.maxRadius();
		this.random = new SplittableRandom(ThreadLocalRandom.current().nextLong());
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
	public SplittableRandom random() { return random; }
}
