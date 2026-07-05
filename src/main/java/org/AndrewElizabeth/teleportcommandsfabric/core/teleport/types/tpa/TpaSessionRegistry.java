package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TpaSessionRegistry {
	private final Map<UUID, Tpa.Session> sessions = new LinkedHashMap<>(1280);
	private final Map<UUID, LinkedHashSet<UUID>> targetIncoming = new HashMap<>(128);

	public void add(Tpa.Session session) {
		if (session == null) {
			return;
		}
		sessions.put(session.sessionId(), session);
		indexIncoming(session);
	}

	public Optional<Tpa.Session> get(UUID sessionId) {
		if (sessionId == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(sessions.get(sessionId));
	}

	public Optional<Tpa.Session> getLatestIncoming(UUID targetUuid) {
		LinkedHashSet<UUID> incoming = targetIncoming.get(targetUuid);
		if (incoming == null || incoming.isEmpty()) {
			return Optional.empty();
		}

		UUID lastId = null;
		for (UUID id : incoming) {
			lastId = id;
		}
		return get(lastId);
	}

	public List<Tpa.Session> getIncoming(UUID targetUuid, long now) {
		LinkedHashSet<UUID> incoming = targetIncoming.get(targetUuid);
		if (incoming == null || incoming.isEmpty()) {
			return List.of();
		}
		return incoming.stream().map(sessions::get)
				.filter(Objects::nonNull)
				.filter(session -> !session.isExpired(now))
				.toList();
	}

	public Optional<Tpa.Session> findIncoming(UUID targetUuid, UUID senderUuid, UUID sessionId, long now) {
		if (targetUuid == null || senderUuid == null) {
			return Optional.empty();
		}
		if (sessionId != null) {
			Tpa.Session session = sessions.get(sessionId);
			if (session == null || !session.target().equals(targetUuid) || !session.sender().equals(senderUuid)
					|| session.isExpired(now)) {
				return Optional.empty();
			}
			return Optional.of(session);
		}
		return getIncoming(targetUuid, now).stream()
				.filter(session -> session.sender().equals(senderUuid))
				.findFirst();
	}

	public boolean hasOutgoing(UUID senderUuid, UUID targetUuid, long now) {
		if (senderUuid == null || targetUuid == null) {
			return false;
		}
		return getIncoming(targetUuid, now).stream()
				.anyMatch(session -> session.sender().equals(senderUuid));
	}

	public Optional<Tpa.Session> remove(UUID sessionId) {
		if (sessionId == null) {
			return Optional.empty();
		}
		Tpa.Session session = sessions.remove(sessionId);
		if (session != null) {
			unindexIncoming(session);
		}
		return Optional.ofNullable(session);
	}

	public List<Tpa.Session> removeForPlayer(UUID playerUuid) {
		if (playerUuid == null || sessions.isEmpty()) {
			return List.of();
		}
		List<Tpa.Session> removed = sessions.values().stream()
				.filter(session -> session.sender().equals(playerUuid) || session.target().equals(playerUuid))
				.toList();
		for (Tpa.Session session : removed) {
			remove(session.sessionId());
		}
		return removed;
	}

	public void clear() {
		sessions.clear();
		targetIncoming.clear();
	}

	private void indexIncoming(Tpa.Session session) {
		targetIncoming.computeIfAbsent(session.target(), ignored -> new LinkedHashSet<>()).add(session.sessionId());
	}

	private void unindexIncoming(Tpa.Session session) {
		LinkedHashSet<UUID> incoming = targetIncoming.get(session.target());
		if (incoming == null) {
			return;
		}
		incoming.remove(session.sessionId());
		if (incoming.isEmpty()) {
			targetIncoming.remove(session.target());
		}
	}
}
