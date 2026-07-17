package org.AndrewElizabeth.teleportcommandsfabric.config.section;

public final class HomeConfig {
	public static final int MIN_PLAYER_MAXIMUM = 0;
	public static final int MIN_TEMPORARY_HOME_TTL_SECONDS = 1;
	public static final int MIN_SHARED_HOME_MAXIMUM = 1;
	public static final int MIN_SHARED_HOME_BROADCAST_COOLDOWN_SECONDS = 10;
	private boolean enabled = true;
	private int playerMaximum = 10;
	private boolean deleteInvalid = false;
	private int temporaryHomeTtlSeconds = 7 * 24 * 60 * 60;
	private int sharedHomeMaximum = 1;
	private int sharedHomeBroadcastCooldownSeconds = 60;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getPlayerMaximum() {
		return playerMaximum;
	}

	public void setPlayerMaximum(int playerMaximum) {
		this.playerMaximum = Math.max(MIN_PLAYER_MAXIMUM, playerMaximum);
	}

	public boolean isDeleteInvalid() {
		return deleteInvalid;
	}

	public void setDeleteInvalid(boolean deleteInvalid) {
		this.deleteInvalid = deleteInvalid;
	}

	public int getTemporaryHomeTtlSeconds() {
		return temporaryHomeTtlSeconds;
	}

	public void setTemporaryHomeTtlSeconds(int temporaryHomeTtlSeconds) {
		this.temporaryHomeTtlSeconds = Math.max(MIN_TEMPORARY_HOME_TTL_SECONDS, temporaryHomeTtlSeconds);
	}

	public int getSharedHomeMaximum() {
		return sharedHomeMaximum;
	}

	public void setSharedHomeMaximum(int sharedHomeMaximum) {
		this.sharedHomeMaximum = Math.max(MIN_SHARED_HOME_MAXIMUM, sharedHomeMaximum);
	}

	public int getSharedHomeBroadcastCooldownSeconds() {
		return sharedHomeBroadcastCooldownSeconds;
	}

	public void setSharedHomeBroadcastCooldownSeconds(int sharedHomeBroadcastCooldownSeconds) {
		this.sharedHomeBroadcastCooldownSeconds = Math.max(MIN_SHARED_HOME_BROADCAST_COOLDOWN_SECONDS,
				sharedHomeBroadcastCooldownSeconds);
	}

	public void normalize() {
		setPlayerMaximum(playerMaximum);
		setTemporaryHomeTtlSeconds(temporaryHomeTtlSeconds);
		setSharedHomeMaximum(sharedHomeMaximum);
		setSharedHomeBroadcastCooldownSeconds(sharedHomeBroadcastCooldownSeconds);
	}
}
