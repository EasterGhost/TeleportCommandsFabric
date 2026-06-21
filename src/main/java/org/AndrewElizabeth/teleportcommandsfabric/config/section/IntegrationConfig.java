package org.AndrewElizabeth.teleportcommandsfabric.config.section;

public final class IntegrationConfig {
	private static final String DEFAULT_SET_NAME = "Default";
	public static final int MIN_SYNC_INTERVAL_SECONDS = 1;
	private boolean enabled = true;
	private int syncIntervalSeconds = 10;
	private boolean persistWaypointSets = true;
	private String warpSetName = DEFAULT_SET_NAME;
	private String homeSetName = DEFAULT_SET_NAME;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getSyncIntervalSeconds() {
		return syncIntervalSeconds;
	}

	public void setSyncIntervalSeconds(int syncIntervalSeconds) {
		this.syncIntervalSeconds = Math.max(MIN_SYNC_INTERVAL_SECONDS, syncIntervalSeconds);
	}

	public boolean isPersistWaypointSets() {
		return persistWaypointSets;
	}

	public void setPersistWaypointSets(boolean persistWaypointSets) {
		this.persistWaypointSets = persistWaypointSets;
	}

	public String getWarpSetName() {
		return warpSetName;
	}

	public void setWarpSetName(String warpSetName) {
		this.warpSetName = warpSetName;
	}

	public String getHomeSetName() {
		return homeSetName;
	}

	public void setHomeSetName(String homeSetName) {
		this.homeSetName = homeSetName;
	}

	public void normalize() {
		setSyncIntervalSeconds(syncIntervalSeconds);
		if (warpSetName == null || warpSetName.isBlank()) {
			warpSetName = DEFAULT_SET_NAME;
		}
		if (homeSetName == null || homeSetName.isBlank()) {
			homeSetName = DEFAULT_SET_NAME;
		}
	}
}
