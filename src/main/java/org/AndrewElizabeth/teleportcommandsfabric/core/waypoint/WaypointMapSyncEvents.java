package org.AndrewElizabeth.teleportcommandsfabric.core.waypoint;

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
			listener.accept(playerUuid);
		}
	}

	public static void markAllDirty() {
		for (Runnable listener : ALL_DIRTY_LISTENERS) {
			listener.run();
		}
	}
}
