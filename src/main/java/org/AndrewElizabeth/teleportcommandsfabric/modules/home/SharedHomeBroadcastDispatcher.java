package org.AndrewElizabeth.teleportcommandsfabric.modules.home;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.core.waypoint.shared.SharedHomeKey;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public final class SharedHomeBroadcastDispatcher {
	private static final int MAX_RECIPIENTS_PER_TICK = 256;

	private final Deque<PendingBroadcast> pendingBroadcasts = new ArrayDeque<>();
	private List<UUID> currentRecipientSnapshot;

	void enqueue(MinecraftServer server, UUID ownerUuid, String ownerName, String homeName, SharedHomeKey key) {
		if (currentRecipientSnapshot == null) {
			currentRecipientSnapshot = server.getPlayerList().getPlayers().stream()
					.map(ServerPlayer::getUUID)
					.toList();
		}
		pendingBroadcasts.addLast(new PendingBroadcast(ownerUuid, ownerName, homeName, key,
				currentRecipientSnapshot));
	}

	public void tick(MinecraftServer server) {
		try {
			int inspected = 0;
			while (inspected < MAX_RECIPIENTS_PER_TICK && !pendingBroadcasts.isEmpty()) {
				PendingBroadcast broadcast = pendingBroadcasts.getFirst();
				if (broadcast.isComplete() || !isCurrent(broadcast.key)) {
					pendingBroadcasts.removeFirst();
					inspected++;
					continue;
				}

				UUID recipientUuid = broadcast.nextRecipient();
				inspected++;
				if (!recipientUuid.equals(broadcast.ownerUuid)) {
					send(server, recipientUuid, broadcast);
				}
				if (broadcast.isComplete()) {
					pendingBroadcasts.removeFirst();
				}
			}
		} finally {
			currentRecipientSnapshot = null;
		}
	}

	public void clear() {
		pendingBroadcasts.clear();
		currentRecipientSnapshot = null;
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
		private final String ownerName;
		private final String homeName;
		private final SharedHomeKey key;
		private final List<UUID> recipients;
		private int nextRecipientIndex;

		private PendingBroadcast(UUID ownerUuid, String ownerName, String homeName, SharedHomeKey key,
				List<UUID> recipients) {
			this.ownerUuid = ownerUuid;
			this.ownerName = ownerName;
			this.homeName = homeName;
			this.key = key;
			this.recipients = recipients;
		}

		private UUID nextRecipient() {
			return recipients.get(nextRecipientIndex++);
		}

		private boolean isComplete() {
			return nextRecipientIndex >= recipients.size();
		}
	}
}
