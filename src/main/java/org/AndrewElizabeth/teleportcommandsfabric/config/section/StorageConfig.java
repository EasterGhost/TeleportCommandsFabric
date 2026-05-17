package org.AndrewElizabeth.teleportcommandsfabric.config.section;

public final class StorageConfig {
	public static final int MIN_AUTO_SAVE_INTERVAL = 1;
	public static final int MAX_AUTO_SAVE_INTERVAL = 300;

	private int autoSaveIntervalSeconds = 15;

	public int getAutoSaveIntervalSeconds() {
		return autoSaveIntervalSeconds;
	}

	public void setAutoSaveIntervalSeconds(int autoSaveIntervalSeconds) {
		this.autoSaveIntervalSeconds = Math.max(MIN_AUTO_SAVE_INTERVAL,
				Math.min(MAX_AUTO_SAVE_INTERVAL, autoSaveIntervalSeconds));
	}

	public void normalize() {
		setAutoSaveIntervalSeconds(autoSaveIntervalSeconds);
	}
}
