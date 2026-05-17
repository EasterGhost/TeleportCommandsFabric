package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task;
 
import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.core.record.AsyncRecordedLocationSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportCooldownManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportPreloadManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
 
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
 
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
 
public final class TeleportExecutor {
	private final AsyncRecordedLocationSource recordedSource;
	private final TeleportCooldownManager cooldownManager;
	private final TeleportPreloadManager preloadManager;
	private final SafetyThreadPool workerPool;
 
	public TeleportExecutor(AsyncRecordedLocationSource recordedSource, TeleportCooldownManager cooldownManager,
			TeleportPreloadManager preloadManager, SafetyThreadPool workerPool) {
		this.recordedSource = recordedSource;
		this.cooldownManager = cooldownManager;
		this.preloadManager = preloadManager;
		this.workerPool = workerPool;
	}
 
	public TeleportStatus executeOne(MinecraftServer server, TeleportBatchDispatcher.ExecutionEntry entry, long currentTick) {
		if (!cooldownManager.isCurrent(entry.playerUuid(), entry.pendingSequence())) {
			preloadManager.release(entry.playerUuid(), entry.pendingSequence());
			entry.resultFuture().complete(TeleportStatus.CANCELLED);
			return TeleportStatus.CANCELLED;
		}
 
		ServerPlayer player = server.getPlayerList().getPlayer(entry.playerUuid());
		if (player == null) {
			return finishEntry(entry, TeleportStatus.PLAYER_DISCONNECTED);
		}
		if (player.isDeadOrDying()) {
			return finishEntry(entry, TeleportStatus.CANCELLED_BY_EVENT);
		}
 
		TeleportTarget target = entry.target();
		ServerLevel world = target.world();
		if (server.getLevel(world.dimension()) == null) {
			return finishEntry(entry, TeleportStatus.TARGET_UNAVAILABLE);
		}
 
		if (!preloadManager.isChunkLoaded(target)) {
			preloadManager.preload(entry, currentTick);
			return TeleportStatus.ACCEPTED;
		}
 
		Vec3 destination = target.position();
		if (entry.options().safetyEnabled()) {
			Optional<BlockPos> safePos = TeleportSafety.getSafeBlockPos(BlockPos.containing(destination), world);
			if (safePos.isEmpty()) {
				return finishEntry(entry, TeleportStatus.NO_SAFE_POSITION);
			}
			BlockPos pos = safePos.get();
			destination = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
		}
 
		if (entry.options().recordPrevious() && recordedSource != null) {
			recordedSource.recordPreviousTeleportLocation(player.getUUID(), player.blockPosition(), player.level().dimension())
					.whenComplete((ignored, throwable) -> {
						if (throwable != null) {
							ModConstants.LOGGER.warn("Failed to record previous teleport location", throwable);
						}
					});
		}
 
		boolean teleported = player.teleportTo(world, destination.x(), destination.y(), destination.z(), Set.of(), player.getYRot(), player.getXRot(), false);
		if (!teleported) {
			return finishEntry(entry, TeleportStatus.FAILED);
		}
 
		cooldownManager.markSuccess(entry.playerUuid(), entry.pendingSequence());
		entry.resultFuture().complete(TeleportStatus.SUCCESS);
		preloadManager.release(entry.playerUuid(), entry.pendingSequence());
		return TeleportStatus.SUCCESS;
	}
 
	public void executeBatch(MinecraftServer server, List<TeleportBatchDispatcher.ExecutionEntry> entries, long currentTick) {
		List<PreparedSafetyCheck> safetyChecks = null;
		for (TeleportBatchDispatcher.ExecutionEntry entry : entries) {
			PreparedExecution prepared = prepareBatchExecution(server, entry, currentTick);
			if (prepared == null) {
				continue;
			}
			if (!entry.options().safetyEnabled()) {
				finishPreparedTeleport(prepared, prepared.destination());
				continue;
			}
 
			BlockPos basePos = BlockPos.containing(prepared.destination());
			CompletableFuture<Optional<BlockPos>> safetyFuture = CompletableFuture.supplyAsync(
					() -> TeleportSafety.getSafeBlockPos(basePos, prepared.world()),
					workerPool.getExecutor());
			if (safetyChecks == null) {
				safetyChecks = new ArrayList<>();
			}
			safetyChecks.add(new PreparedSafetyCheck(prepared, basePos, safetyFuture));
		}
 
		if (safetyChecks == null) {
			return;
		}
 
		for (PreparedSafetyCheck safetyCheck : safetyChecks) {
			Optional<BlockPos> safePos = joinSafetyCheck(safetyCheck);
			if (safePos.isEmpty()) {
				finishEntry(safetyCheck.prepared().entry(), TeleportStatus.NO_SAFE_POSITION);
				continue;
			}
 
			BlockPos pos = safePos.get();
			Vec3 destination = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
			finishPreparedTeleport(safetyCheck.prepared(), destination);
		}
	}
 
	private PreparedExecution prepareBatchExecution(MinecraftServer server, TeleportBatchDispatcher.ExecutionEntry entry, long currentTick) {
		if (!cooldownManager.isCurrent(entry.playerUuid(), entry.pendingSequence())) {
			preloadManager.release(entry.playerUuid(), entry.pendingSequence());
			entry.resultFuture().complete(TeleportStatus.CANCELLED);
			return null;
		}
 
		ServerPlayer player = server.getPlayerList().getPlayer(entry.playerUuid());
		if (player == null) {
			finishEntry(entry, TeleportStatus.PLAYER_DISCONNECTED);
			return null;
		}
		if (player.isDeadOrDying()) {
			finishEntry(entry, TeleportStatus.CANCELLED_BY_EVENT);
			return null;
		}
 
		TeleportTarget target = entry.target();
		ServerLevel world = target.world();
		if (server.getLevel(world.dimension()) == null) {
			finishEntry(entry, TeleportStatus.TARGET_UNAVAILABLE);
			return null;
		}
 
		if (!preloadManager.isChunkLoaded(target)) {
			preloadManager.preload(entry, currentTick);
			return null;
		}
 
		return new PreparedExecution(server, entry, world, target.position());
	}
 
	private Optional<BlockPos> joinSafetyCheck(PreparedSafetyCheck safetyCheck) {
		try {
			return safetyCheck.safetyFuture().join();
		} catch (CancellationException | CompletionException exception) {
			ModConstants.LOGGER.warn("Parallel teleport safety check failed; falling back to server thread", exception);
			return TeleportSafety.getSafeBlockPos(safetyCheck.basePos(), safetyCheck.prepared().world());
		}
	}
 
	private TeleportStatus finishPreparedTeleport(PreparedExecution prepared, Vec3 destination) {
		TeleportBatchDispatcher.ExecutionEntry entry = prepared.entry();
		if (!cooldownManager.isCurrent(entry.playerUuid(), entry.pendingSequence())) {
			preloadManager.release(entry.playerUuid(), entry.pendingSequence());
			entry.resultFuture().complete(TeleportStatus.CANCELLED);
			return TeleportStatus.CANCELLED;
		}
 
		ServerPlayer player = prepared.server().getPlayerList().getPlayer(entry.playerUuid());
		if (player == null) {
			return finishEntry(entry, TeleportStatus.PLAYER_DISCONNECTED);
		}
		if (player.isDeadOrDying()) {
			return finishEntry(entry, TeleportStatus.CANCELLED_BY_EVENT);
		}
		if (prepared.server().getLevel(prepared.world().dimension()) == null) {
			return finishEntry(entry, TeleportStatus.TARGET_UNAVAILABLE);
		}
 
		if (entry.options().recordPrevious() && recordedSource != null) {
			recordedSource.recordPreviousTeleportLocation(player.getUUID(), player.blockPosition(), player.level().dimension())
					.whenComplete((ignored, throwable) -> {
						if (throwable != null) {
							ModConstants.LOGGER.warn("Failed to record previous teleport location", throwable);
						}
					});
		}
 
		boolean teleported = player.teleportTo(prepared.world(), destination.x(), destination.y(), destination.z(), Set.of(),
				player.getYRot(), player.getXRot(), false);
		if (!teleported) {
			return finishEntry(entry, TeleportStatus.FAILED);
		}
 
		cooldownManager.markSuccess(entry.playerUuid(), entry.pendingSequence());
		entry.resultFuture().complete(TeleportStatus.SUCCESS);
		preloadManager.release(entry.playerUuid(), entry.pendingSequence());
		return TeleportStatus.SUCCESS;
	}
 
	public TeleportStatus finishEntry(TeleportBatchDispatcher.ExecutionEntry entry, TeleportStatus status) {
		if (status == TeleportStatus.SUCCESS) {
			cooldownManager.markSuccess(entry.playerUuid(), entry.pendingSequence());
			entry.resultFuture().complete(TeleportStatus.SUCCESS);
		} else {
			cooldownManager.cancelPending(entry.playerUuid(), entry.pendingSequence(), status);
			entry.resultFuture().complete(status);
		}
		preloadManager.release(entry.playerUuid(), entry.pendingSequence());
		return status;
	}
 
	private record PreparedExecution(
			MinecraftServer server,
			TeleportBatchDispatcher.ExecutionEntry entry,
			ServerLevel world,
			Vec3 destination) {
	}
 
	private record PreparedSafetyCheck(
			PreparedExecution prepared,
			BlockPos basePos,
			CompletableFuture<Optional<BlockPos>> safetyFuture) {
	}
}
