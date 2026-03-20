package org.AndrewElizabeth.teleportcommandsfabric.storage;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportCooldownManager {
	private static final Map<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> scheduledTeleports = new ConcurrentHashMap<>();

	public static int getRemainingCooldown(UUID playerUuid, int cooldownSeconds) {
		if (!lastTeleportTime.containsKey(playerUuid)) {
			return 0;
		}

		long lastTime = lastTeleportTime.get(playerUuid);
		long currentTime = System.currentTimeMillis();
		long elapsedSeconds = (currentTime - lastTime) / 1000;

		int remaining = cooldownSeconds - (int) elapsedSeconds;
		return Math.max(0, remaining);
	}

	public static void updateLastTeleportTime(UUID playerUuid) {
		lastTeleportTime.put(playerUuid, System.currentTimeMillis());
	}

	public static long scheduleTeleport(UUID playerUuid) {
		long teleportId = System.currentTimeMillis();
		scheduledTeleports.put(playerUuid, teleportId);
		return teleportId;
	}

	public static boolean isScheduledTeleportValid(UUID playerUuid, long teleportId) {
		Long scheduled = scheduledTeleports.get(playerUuid);
		return scheduled != null && scheduled == teleportId;
	}

	public static void cancelScheduledTeleport(UUID playerUuid) {
		scheduledTeleports.remove(playerUuid);
	}

	public static void removePlayer(UUID playerUuid) {
		scheduledTeleports.remove(playerUuid);
		lastTeleportTime.remove(playerUuid);
	}

	public static void clearAll() {
		lastTeleportTime.clear();
		scheduledTeleports.clear();
	}
}
