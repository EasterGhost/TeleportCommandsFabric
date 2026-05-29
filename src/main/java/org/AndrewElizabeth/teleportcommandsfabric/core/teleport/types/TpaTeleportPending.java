package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TpaTeleportPending implements TeleportOperation {
	private final UUID playerUuid;
	private final long pendingSequence;
	private final long createTick;
	private final int delayTicks;
	private final long cooldownMillis;
	private final boolean recordPrevious;
	private final CompletableFuture<TeleportStatus> resultFuture = new CompletableFuture<>();
	private final UUID sessionId;
	private final UUID senderUuid;
	private final UUID targetUuid;
	private final UUID destinationPlayerUuid;
	private final Tpa.Type type;

	public TpaTeleportPending(UUID playerUuid, long pendingSequence, long createTick, int delayTicks,
			long cooldownMillis, boolean recordPrevious, UUID sessionId, UUID senderUuid, UUID targetUuid,
			UUID destinationPlayerUuid, Tpa.Type type) {
		this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
		this.pendingSequence = pendingSequence;
		this.createTick = createTick;
		this.delayTicks = delayTicks;
		this.cooldownMillis = cooldownMillis;
		this.recordPrevious = recordPrevious;
		this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
		this.senderUuid = Objects.requireNonNull(senderUuid, "senderUuid");
		this.targetUuid = Objects.requireNonNull(targetUuid, "targetUuid");
		this.destinationPlayerUuid = Objects.requireNonNull(destinationPlayerUuid, "destinationPlayerUuid");
		this.type = Objects.requireNonNull(type, "type");
	}

	public static TpaTeleportPending fromSession(Tpa.Session session, long pendingSequence, long createTick) {
		Objects.requireNonNull(session, "session");
		return fromSession(session, pendingSequence, createTick,
				session.delayTicks(), session.cooldownMillis(), session.recordPrevious());
	}
	public static TpaTeleportPending fromSession(Tpa.Session session, long pendingSequence, long createTick,
			int delayTicks, long cooldownMillis, boolean recordPrevious) {
		Objects.requireNonNull(session, "session");
		UUID playerToMove = session.type() == Tpa.Type.TPA ? session.sender() : session.target();
		UUID destinationPlayer = session.type() == Tpa.Type.TPA ? session.target() : session.sender();
		return new TpaTeleportPending(playerToMove, pendingSequence, createTick, delayTicks, cooldownMillis,
				recordPrevious, session.sessionId(), session.sender(), session.target(), destinationPlayer, session.type());
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

	public UUID sessionId() { return sessionId; }
	public UUID senderUuid() { return senderUuid; }
	public UUID targetUuid() { return targetUuid; }
	public UUID destinationPlayerUuid() { return destinationPlayerUuid; }
	public Tpa.Type type() { return type; }
}
