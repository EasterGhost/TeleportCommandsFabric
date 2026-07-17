package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeKey;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public final class SharedHomeBroadcastDispatcher {
	private static final int MAX_RECIPIENTS_PER_TICK = 256;
	private static final int BURST_BUFFER_SECONDS = 5 * 60;
	private static final long MAX_PENDING_RECIPIENTS =
			(long) MAX_RECIPIENTS_PER_TICK * 20L * BURST_BUFFER_SECONDS;
	private static final long MAX_BROADCAST_AGE_MILLIS = BURST_BUFFER_SECONDS * 1000L;

	private final LinkedHashMap<SharedHomeKey, PendingBroadcast> pendingBroadcasts = new LinkedHashMap<>();
	private List<UUID> currentRecipientSnapshot;
	private long pendingRecipientCount;

	void enqueue(MinecraftServer server, UUID ownerUuid, String ownerName, String homeName, SharedHomeKey key) {
		long now = Util.getMillis();
		PendingBroadcast existing = pendingBroadcasts.remove(key);
		if (existing != null) {
			existing.update(ownerName, homeName, now);
			pendingBroadcasts.put(key, existing);
			return;
		}

		if (currentRecipientSnapshot == null) {
			currentRecipientSnapshot = server.getPlayerList().getPlayers().stream()
					.map(ServerPlayer::getUUID)
					.toList();
		}

		PendingBroadcast broadcast = new PendingBroadcast(ownerUuid, ownerName, homeName, key,
				currentRecipientSnapshot, now);
		makeRoomFor(broadcast.remainingRecipients());
		pendingBroadcasts.put(key, broadcast);
		pendingRecipientCount += broadcast.remainingRecipients();
	}

	public void tick(MinecraftServer server) {
		try {
			int inspected = 0;
			long now = Util.getMillis();
			while (inspected < MAX_RECIPIENTS_PER_TICK && !pendingBroadcasts.isEmpty()) {
				PendingBroadcast broadcast = firstPendingBroadcast();
				if (broadcast.isComplete() || broadcast.isExpired(now) || !isCurrent(broadcast.key)) {
					remove(broadcast);
					inspected++;
					continue;
				}

				UUID recipientUuid = broadcast.nextRecipient();
				pendingRecipientCount--;
				inspected++;
				if (!recipientUuid.equals(broadcast.ownerUuid)) {
					send(server, recipientUuid, broadcast);
				}
				if (broadcast.isComplete()) {
					remove(broadcast);
				}
			}
		} finally {
			currentRecipientSnapshot = null;
		}
	}

	public void clear() {
		pendingBroadcasts.clear();
		currentRecipientSnapshot = null;
		pendingRecipientCount = 0L;
	}

	private void makeRoomFor(int recipients) {
		while (!pendingBroadcasts.isEmpty() && pendingRecipientCount + recipients > MAX_PENDING_RECIPIENTS) {
			remove(firstPendingBroadcast());
		}
	}

	private PendingBroadcast firstPendingBroadcast() {
		return pendingBroadcasts.entrySet().iterator().next().getValue();
	}

	private void remove(PendingBroadcast broadcast) {
		if (pendingBroadcasts.remove(broadcast.key, broadcast)) {
			pendingRecipientCount -= broadcast.remainingRecipients();
		}
	}

	private static boolean isCurrent(SharedHomeKey key) {
		return TeleportCommands.SHARED_HOME_SERVICE != null
				&& TeleportCommands.SHARED_HOME_SERVICE.isPublished(key);
	}

	private static void send(MinecraftServer server, UUID recipientUuid, PendingBroadcast broadcast) {
		ServerPlayer recipient = server.getPlayerList().getPlayer(recipientUuid);
		if (recipient == null) {
			return;
		}
		try {
			SharedHomeMessages.sendBroadcast(recipient, broadcast.ownerName, broadcast.homeName, broadcast.key);
		} catch (RuntimeException exception) {
			ModConstants.LOGGER.error("Failed to send a shared home broadcast to {}.", recipientUuid, exception);
		}
	}

	private static final class PendingBroadcast {
		private final UUID ownerUuid;
		private final SharedHomeKey key;
		private final List<UUID> recipients;
		private String ownerName;
		private String homeName;
		private long enqueuedAtMillis;
		private int nextRecipientIndex;

		private PendingBroadcast(UUID ownerUuid, String ownerName, String homeName, SharedHomeKey key,
				List<UUID> recipients, long enqueuedAtMillis) {
			this.ownerUuid = ownerUuid;
			this.ownerName = ownerName;
			this.homeName = homeName;
			this.key = key;
			this.recipients = recipients;
			this.enqueuedAtMillis = enqueuedAtMillis;
		}

		private void update(String ownerName, String homeName, long enqueuedAtMillis) {
			this.ownerName = ownerName;
			this.homeName = homeName;
			this.enqueuedAtMillis = enqueuedAtMillis;
		}

		private UUID nextRecipient() {
			return recipients.get(nextRecipientIndex++);
		}

		private boolean isComplete() {
			return nextRecipientIndex >= recipients.size();
		}

		private int remainingRecipients() {
			return recipients.size() - nextRecipientIndex;
		}

		private boolean isExpired(long nowMillis) {
			return nowMillis - enqueuedAtMillis >= MAX_BROADCAST_AGE_MILLIS;
		}
	}
}
