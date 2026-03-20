package org.AndrewElizabeth.teleportcommandsfabric.storage;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LogoutCacheManager {
	private static final ScheduledExecutorService CLEANUP_SCHEDULER = Executors.newSingleThreadScheduledExecutor(
			runnable -> {
				Thread thread = new Thread(runnable, "teleportcommands-logout-cleanup");
				thread.setDaemon(true);
				return thread;
			});

	private static final Map<UUID, ScheduledFuture<?>> scheduledCleanups = new ConcurrentHashMap<>();
	private static final long CLEANUP_DELAY_MINUTES = 30;

	public static void scheduleCleanup(UUID playerUuid) {
		cancelCleanup(playerUuid);

		Runnable cleanupTask = () -> {
			if (TeleportCommands.SERVER != null) {
				TeleportCommands.SERVER.execute(() -> {
					PreviousTeleportLocationStorage.removePreviousTeleportLocation(playerUuid);
					TeleportCooldownManager.removePlayer(playerUuid);
					scheduledCleanups.remove(playerUuid);
				});
			} else {
				PreviousTeleportLocationStorage.removePreviousTeleportLocation(playerUuid);
				TeleportCooldownManager.removePlayer(playerUuid);
				scheduledCleanups.remove(playerUuid);
			}
		};

		ScheduledFuture<?> future = CLEANUP_SCHEDULER.schedule(cleanupTask, CLEANUP_DELAY_MINUTES, TimeUnit.MINUTES);
		scheduledCleanups.put(playerUuid, future);
	}

	public static void cancelCleanup(UUID playerUuid) {
		ScheduledFuture<?> future = scheduledCleanups.remove(playerUuid);
		if (future != null) {
			future.cancel(false);
		}
	}
}
