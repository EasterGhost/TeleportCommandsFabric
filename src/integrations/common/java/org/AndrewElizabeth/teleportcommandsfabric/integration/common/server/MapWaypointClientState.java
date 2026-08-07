package org.AndrewElizabeth.teleportcommandsfabric.integration.common.server;

import org.AndrewElizabeth.teleportcommandsfabric.integration.common.waypoint.MapWaypointSnapshot;

final class MapWaypointClientState {
	private volatile MapWaypointSyncMode syncMode;
	private volatile int protocolVersion;
	private volatile boolean dirty;
	private volatile boolean flushInProgress;
	private volatile long lastFlushTimeMillis;
	private volatile MapWaypointSnapshot lastSnapshot;
	private volatile long nextHomeExpiryMillis;

	MapWaypointClientState(MapWaypointSyncMode syncMode, int protocolVersion) {
		this.syncMode = syncMode;
		this.protocolVersion = protocolVersion;
	}

	MapWaypointSyncMode syncMode() {
		return syncMode;
	}

	int protocolVersion() {
		return protocolVersion;
	}

	boolean useCommon(int protocolVersion) {
		if (syncMode == MapWaypointSyncMode.COMMON && protocolVersion <= this.protocolVersion) {
			return false;
		}
		this.syncMode = MapWaypointSyncMode.COMMON;
		this.protocolVersion = protocolVersion;
		this.lastSnapshot = null;
		return true;
	}

	void useLegacyXaero() {
		if (syncMode != MapWaypointSyncMode.COMMON) {
			syncMode = MapWaypointSyncMode.LEGACY_XAERO;
		}
	}

	void markDirty() {
		dirty = true;
	}

	void markDirtyIfHomeExpired(long nowMillis) {
		long expiryMillis = nextHomeExpiryMillis;
		if (expiryMillis <= 0L || nowMillis < expiryMillis) {
			return;
		}
		nextHomeExpiryMillis = 0L;
		markDirty();
	}

	boolean shouldFlush(long now, long intervalMs) {
		return dirty && !flushInProgress && now - lastFlushTimeMillis >= intervalMs;
	}

	void beginFlush() {
		flushInProgress = true;
		dirty = false;
	}

	boolean isSnapshotChanged(MapWaypointSnapshot snapshot) {
		return !snapshot.equals(lastSnapshot);
	}

	void updateSnapshot(MapWaypointSnapshot snapshot) {
		lastSnapshot = snapshot;
	}

	void updateNextHomeExpiry(long expiryMillis) {
		nextHomeExpiryMillis = expiryMillis;
	}

	void finishFlush(long now) {
		lastFlushTimeMillis = now;
		flushInProgress = false;
	}
}
