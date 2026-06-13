package org.AndrewElizabeth.teleportcommandsfabric.config.section;

public final class TeleportingConfig {
	public static final int MIN_DELAY = 0;
	public static final int MIN_COOLDOWN = 0;
	public static final int MIN_PRELOAD_RADIUS_CHUNKS = 0;
	private int delay = 0;
	private int cooldown = 3;
	private boolean preloadEnabled = false;
	private int preloadRadiusChunks = 1;
	private boolean defaultSafetyCheck = false;
	private boolean teleportEffects = true;
	private boolean restoreRotation = true;

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = Math.max(MIN_DELAY, delay);
	}

	public int getCooldown() {
		return cooldown;
	}

	public void setCooldown(int cooldown) {
		this.cooldown = Math.max(MIN_COOLDOWN, cooldown);
	}

	public boolean isPreloadEnabled() {
		return preloadEnabled;
	}

	public void setPreloadEnabled(boolean preloadEnabled) {
		this.preloadEnabled = preloadEnabled;
	}

	public int getPreloadRadiusChunks() {
		return preloadRadiusChunks;
	}

	public void setPreloadRadiusChunks(int preloadRadiusChunks) {
		this.preloadRadiusChunks = Math.max(MIN_PRELOAD_RADIUS_CHUNKS, preloadRadiusChunks);
	}

	public boolean isDefaultSafetyCheck() {
		return defaultSafetyCheck;
	}

	public void setDefaultSafetyCheck(boolean defaultSafetyCheck) {
		this.defaultSafetyCheck = defaultSafetyCheck;
	}

	public boolean isTeleportEffects() {
		return teleportEffects;
	}

	public void setTeleportEffects(boolean teleportEffects) {
		this.teleportEffects = teleportEffects;
	}

	public boolean isRestoreRotation() {
		return restoreRotation;
	}

	public void setRestoreRotation(boolean restoreRotation) {
		this.restoreRotation = restoreRotation;
	}

	public void normalize() {
		setDelay(delay);
		setCooldown(cooldown);
		setPreloadRadiusChunks(preloadRadiusChunks);
	}
}
