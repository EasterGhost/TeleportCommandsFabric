package org.AndrewElizabeth.teleportcommandsfabric.storage;

import java.util.HashMap;

public class TeleportCooldownManager {
	private static final HashMap<String, Long> lastTeleportTime = new HashMap<>();
	private static final HashMap<String, Long> scheduledTeleports = new HashMap<>();

	/**
	 * Check if a player has an active teleport cooldown
	 * 
	 * @param uuid            Player UUID
	 * @param cooldownSeconds Required cooldown in seconds
	 * @return Remaining cooldown time in seconds, or 0 if no cooldown
	 */
	public static int getRemainingCooldown(String uuid, int cooldownSeconds) {
		if (!lastTeleportTime.containsKey(uuid)) {
			return 0;
		}

		long lastTime = lastTeleportTime.get(uuid);
		long currentTime = System.currentTimeMillis();
		long elapsedSeconds = (currentTime - lastTime) / 1000;

		int remaining = cooldownSeconds - (int) elapsedSeconds;
		return Math.max(0, remaining);
	}

	/**
	 * Update the last teleport time for a player
	 * 
	 * @param uuid Player UUID
	 */
	public static void updateLastTeleportTime(String uuid) {
		lastTeleportTime.put(uuid, System.currentTimeMillis());
	}

	/**
	 * Schedule a teleport for a player (for delay system)
	 * 
	 * @param uuid Player UUID
	 * @return Teleport ID (timestamp)
	 */
	public static long scheduleTeleport(String uuid) {
		long teleportId = System.currentTimeMillis();
		scheduledTeleports.put(uuid, teleportId);
		return teleportId;
	}

	/**
	 * Check if a scheduled teleport is still valid
	 * 
	 * @param uuid       Player UUID
	 * @param teleportId Teleport ID to validate
	 * @return true if valid, false if cancelled or replaced
	 */
	public static boolean isScheduledTeleportValid(String uuid, long teleportId) {
		Long scheduled = scheduledTeleports.get(uuid);
		return scheduled != null && scheduled == teleportId;
	}

	/**
	 * Cancel any scheduled teleport for a player
	 * 
	 * @param uuid Player UUID
	 */
	public static void cancelScheduledTeleport(String uuid) {
		scheduledTeleports.remove(uuid);
	}

	/**
	 * Clear all cooldowns and scheduled teleports
	 */
	public static void clearAll() {
		lastTeleportTime.clear();
		scheduledTeleports.clear();
	}
}
