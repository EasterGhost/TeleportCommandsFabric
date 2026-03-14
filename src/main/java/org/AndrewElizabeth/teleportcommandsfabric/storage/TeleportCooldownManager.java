package org.AndrewElizabeth.teleportcommandsfabric.storage;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportCooldownManager {
	private static final Map<UUID, Long> lastTeleportTime = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> scheduledTeleports = new ConcurrentHashMap<>();

	/**
	 * Check if a player has an active teleport cooldown
	 * 
	 * @param playerUuid      Player UUID
	 * @param cooldownSeconds Required cooldown in seconds
	 * @return Remaining cooldown time in seconds, or 0 if no cooldown
	 */
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

	/**
	 * Update the last teleport time for a player
	 * 
	 * @param playerUuid Player UUID
	 */
	public static void updateLastTeleportTime(UUID playerUuid) {
		lastTeleportTime.put(playerUuid, System.currentTimeMillis());
	}

	/**
	 * Schedule a teleport for a player (for delay system)
	 * 
	 * @param playerUuid Player UUID
	 * @return Teleport ID (timestamp)
	 */
	public static long scheduleTeleport(UUID playerUuid) {
		long teleportId = System.currentTimeMillis();
		scheduledTeleports.put(playerUuid, teleportId);
		return teleportId;
	}

	/**
	 * Check if a scheduled teleport is still valid
	 * 
	 * @param playerUuid Player UUID
	 * @param teleportId Teleport ID to validate
	 * @return true if valid, false if cancelled or replaced
	 */
	public static boolean isScheduledTeleportValid(UUID playerUuid, long teleportId) {
		Long scheduled = scheduledTeleports.get(playerUuid);
		return scheduled != null && scheduled == teleportId;
	}

	/**
	 * Cancel any scheduled teleport for a player
	 * 
	 * @param playerUuid Player UUID
	 */
	public static void cancelScheduledTeleport(UUID playerUuid) {
		scheduledTeleports.remove(playerUuid);
	}

	/**
	 * Clear all cooldowns and scheduled teleports
	 */
	public static void clearAll() {
		lastTeleportTime.clear();
		scheduledTeleports.clear();
	}
}
