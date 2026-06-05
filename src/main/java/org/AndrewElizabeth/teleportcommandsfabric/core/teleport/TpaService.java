package org.AndrewElizabeth.teleportcommandsfabric.core.teleport;

import org.AndrewElizabeth.teleportcommandsfabric.core.record.AsyncRecordedLocationSource;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportOperationManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.manager.TeleportPreloadManager;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TeleportExecutor;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.TpaRequestExpiryScheduler;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportOperation;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportTarget;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.Tpa;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.TpaRequest;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.TpaSessionRegistry;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.TpaTeleportPending;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public final class TpaService {
	private final TeleportOperationManager operationManager;
	private final TeleportPreloadManager preloadManager;
	private final TeleportExecutor executor;
	private final TpaRequestExpiryScheduler expiryScheduler;
	private final BiConsumer<MinecraftServer, Tpa.Session> expirationListener;
	private final TpaSessionRegistry sessions = new TpaSessionRegistry();
	private final ArrayDeque<PendingRef> acceptedQueue = new ArrayDeque<>();
	private long currentTick;

	private static final int TPA_BATCH_LIMIT = 128;

	public TpaService(AsyncRecordedLocationSource recordedSource, TeleportOperationManager operationManager,
			TeleportPreloadManager preloadManager) {
		this(recordedSource, operationManager, preloadManager, new TpaRequestExpiryScheduler(), (server, session) -> {
		});
	}

	public TpaService(AsyncRecordedLocationSource recordedSource, TeleportOperationManager operationManager,
			TeleportPreloadManager preloadManager, BiConsumer<MinecraftServer, Tpa.Session> expirationListener) {
		this(recordedSource, operationManager, preloadManager, new TpaRequestExpiryScheduler(), expirationListener);
	}

	TpaService(AsyncRecordedLocationSource recordedSource, TeleportOperationManager operationManager,
			TeleportPreloadManager preloadManager, TpaRequestExpiryScheduler expiryScheduler,
			BiConsumer<MinecraftServer, Tpa.Session> expirationListener) {
		this.operationManager = Objects.requireNonNull(operationManager, "operationManager");
		this.preloadManager = Objects.requireNonNull(preloadManager, "preloadManager");
		this.executor = new TeleportExecutor(recordedSource, this.operationManager);
		this.expiryScheduler = Objects.requireNonNull(expiryScheduler, "expiryScheduler");
		this.expirationListener = Objects.requireNonNull(expirationListener, "expirationListener");
	}

	public Tpa.Session createRequest(TpaRequest request) {
		Objects.requireNonNull(request, "request");
		UUID sessionId = UUID.randomUUID();
		long expiredTime = Util.getMillis() + request.expiry().toMillis();
		Tpa.Session session = new Tpa.Session(sessionId, request.senderUuid(), request.targetUuid(), request.type(), expiredTime,
				request.delayTicks(), request.cooldownMillis(), request.recordPrevious());

		sessions.add(session);
		expiryScheduler.schedule(sessionId, request.expiry());
		return session;
	}

	public Tpa.Session createRequest(UUID sender, UUID target, Tpa.Type type, Duration expiry) {
		return createRequest(TpaRequest.of(sender, target, type, expiry));
	}

	public Optional<Tpa.Session> getSession(UUID sessionId) {
		return sessions.get(sessionId);
	}

	public Optional<Tpa.Session> getLatestIncoming(UUID targetUuid) {
		return sessions.getLatestIncoming(targetUuid);
	}

	public List<Tpa.Session> getIncoming(UUID targetUuid) {
		return sessions.getIncoming(targetUuid, Util.getMillis());
	}

	public Optional<Tpa.Session> findIncoming(UUID targetUuid, UUID senderUuid, UUID sessionId) {
		long now = Util.getMillis();
		Optional<Tpa.Session> session = sessions.findIncoming(targetUuid, senderUuid, sessionId, now);
		if (session.isEmpty() && sessionId != null) {
			removeExpiredIfPresent(sessionId, now);
		}
		return session;
	}

	public boolean hasOutgoing(UUID senderUuid, UUID targetUuid) {
		return sessions.hasOutgoing(senderUuid, targetUuid, Util.getMillis());
	}

	public CompletableFuture<TeleportStatus> acceptRequest(MinecraftServer server, UUID sessionId) {
		if (server == null) {
			return CompletableFuture.completedFuture(TeleportStatus.SERVER_UNAVAILABLE);
		}
		if (sessionId == null) {
			return CompletableFuture.completedFuture(TeleportStatus.TARGET_UNAVAILABLE);
		}

		Optional<Tpa.Session> sessionOpt = sessions.get(sessionId);
		if (sessionOpt.isEmpty()) {
			return CompletableFuture.completedFuture(TeleportStatus.TARGET_UNAVAILABLE);
		}
		Tpa.Session session = sessionOpt.get();
		if (session.isExpired(Util.getMillis())) {
			remove(sessionId);
			return CompletableFuture.completedFuture(TeleportStatus.TARGET_UNAVAILABLE);
		}

		UUID playerToMoveUuid = session.type() == Tpa.Type.TPA ? session.sender() : session.target();
		UUID destinationPlayerUuid = session.type() == Tpa.Type.TPA ? session.target() : session.sender();
		ServerPlayer playerToMove = server.getPlayerList().getPlayer(playerToMoveUuid);
		ServerPlayer destinationPlayer = server.getPlayerList().getPlayer(destinationPlayerUuid);
		if (playerToMove == null || destinationPlayer == null) {
			remove(sessionId);
			return CompletableFuture.completedFuture(TeleportStatus.PLAYER_DISCONNECTED);
		}
		if (playerToMove.isDeadOrDying() || destinationPlayer.isDeadOrDying()) {
			remove(sessionId);
			return CompletableFuture.completedFuture(TeleportStatus.CANCELLED_BY_EVENT);
		}

		long remainingCooldown = operationManager.getRemainingCooldownMillis(playerToMoveUuid, session.cooldownMillis());
		if (remainingCooldown > 0L) {
			return CompletableFuture.completedFuture(TeleportStatus.COOLDOWN);
		}

		TeleportOperationManager.OperationCreateResult<TpaTeleportPending> createResult = operationManager.createOperation(playerToMoveUuid,
				currentTick, (sequence, tick) -> TpaTeleportPending.fromSession(session, sequence, tick));
		createResult.replaced().ifPresent(this::releaseReplacedTargetPreload);
		acceptedQueue.addLast(new PendingRef(playerToMoveUuid, createResult.pending().pendingSequence()));
		remove(sessionId);
		return createResult.pending().resultFuture();
	}

	public void remove(UUID sessionId) {
		if (sessionId == null) {
			return;
		}
		expiryScheduler.cancel(sessionId);
		sessions.remove(sessionId);
	}

	public void tick(MinecraftServer server) {
		if (server == null) {
			return;
		}
		currentTick++;
		advancePending(server);
		expiryScheduler.drainExpired(sessionId -> expireSession(server, sessionId));
	}

	public void onPlayerQuit(UUID playerUuid) {
		if (playerUuid == null) {
			return;
		}

		for (Tpa.Session session : sessions.removeForPlayer(playerUuid)) {
			expiryScheduler.cancel(session.sessionId());
		}

		for (TpaTeleportPending pending : operationManager.currentOperations(TpaTeleportPending.class)) {
			if (pending.playerUuid().equals(playerUuid) || pending.destinationPlayerUuid().equals(playerUuid)) {
				executor.finishOperation(pending, TeleportStatus.CANCELLED);
			}
		}
		acceptedQueue.removeIf(ref -> ref.playerUuid().equals(playerUuid));
	}

	public void clear() {
		expiryScheduler.cancelAll();
		sessions.clear();
		acceptedQueue.clear();
		for (TpaTeleportPending pending : operationManager.currentOperations(TpaTeleportPending.class)) {
			executor.finishOperation(pending, TeleportStatus.CANCELLED);
		}
	}

	public void shutdown() {
		clear();
		expiryScheduler.shutdown();
	}

	private void advancePending(MinecraftServer server) {
		int executed = 0;
		while (executed < TPA_BATCH_LIMIT && !acceptedQueue.isEmpty()) {
			PendingRef ref = acceptedQueue.peekFirst();
			Optional<TpaTeleportPending> pending = operationManager.getCurrentOperation(ref.playerUuid(), TpaTeleportPending.class)
					.filter(operation -> operation.pendingSequence() == ref.pendingSequence());
			if (pending.isEmpty()) {
				acceptedQueue.pollFirst();
				continue;
			}
			if (!pending.get().isDelayDone(currentTick)) {
				break;
			}

			acceptedQueue.pollFirst();
			executePending(server, pending.get());
			executed++;
		}
	}

	private void executePending(MinecraftServer server, TpaTeleportPending pending) {
		if (!operationManager.isCurrent(pending)) {
			pending.resultFuture().complete(TeleportStatus.CANCELLED);
			return;
		}

		ServerPlayer destinationPlayer = server.getPlayerList().getPlayer(pending.destinationPlayerUuid());
		if (destinationPlayer == null) {
			executor.finishOperation(pending, TeleportStatus.PLAYER_DISCONNECTED);
			return;
		}
		if (destinationPlayer.isDeadOrDying()) {
			executor.finishOperation(pending, TeleportStatus.CANCELLED_BY_EVENT);
			return;
		}

		TeleportTarget target = TeleportTarget.of((ServerLevel) destinationPlayer.level(), destinationPlayer.position());
		executor.executeResolved(server, pending, target);
	}

	private void expireSession(MinecraftServer server, UUID sessionId) {
		long now = Util.getMillis();
		Optional<Tpa.Session> session = sessions.get(sessionId);
		if (session.isEmpty() || !session.get().isExpired(now)) {
			return;
		}
		remove(sessionId);
		expirationListener.accept(server, session.get());
	}

	private void removeExpiredIfPresent(UUID sessionId, long now) {
		sessions.get(sessionId)
				.filter(session -> session.isExpired(now))
				.ifPresent(session -> remove(sessionId));
	}

	private record PendingRef(UUID playerUuid, long pendingSequence) {
	}

	private void releaseReplacedTargetPreload(TeleportOperation replaced) {
		preloadManager.release(replaced.playerUuid(), replaced.pendingSequence());
	}
}
