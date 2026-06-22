package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class WaypointMapSyncEvents {
	private static final List<Consumer<UUID>> PLAYER_DIRTY_LISTENERS = new CopyOnWriteArrayList<>();
	private static final List<Runnable> ALL_DIRTY_LISTENERS = new CopyOnWriteArrayList<>();

	private WaypointMapSyncEvents() {
	}

	public static void registerPlayerDirtyListener(Consumer<UUID> listener) {
		PLAYER_DIRTY_LISTENERS.add(listener);
	}

	public static void registerAllDirtyListener(Runnable listener) {
		ALL_DIRTY_LISTENERS.add(listener);
	}

	public static void markPlayerDirty(UUID playerUuid) {
		if (playerUuid == null) {
			return;
		}
		for (Consumer<UUID> listener : PLAYER_DIRTY_LISTENERS) {
			notifyPlayerDirty(listener, playerUuid);
		}
	}

	public static void markAllDirty() {
		for (Runnable listener : ALL_DIRTY_LISTENERS) {
			notifyAllDirty(listener);
		}
	}

	private static void notifyPlayerDirty(Consumer<UUID> listener, UUID playerUuid) {
		try {
			listener.accept(playerUuid);
		} catch (RuntimeException exception) {
			ModConstants.LOGGER.error("Waypoint map sync player dirty listener failed.", exception);
		}
	}

	private static void notifyAllDirty(Runnable listener) {
		try {
			listener.run();
		} catch (RuntimeException exception) {
			ModConstants.LOGGER.error("Waypoint map sync all dirty listener failed.", exception);
		}
	}
}
