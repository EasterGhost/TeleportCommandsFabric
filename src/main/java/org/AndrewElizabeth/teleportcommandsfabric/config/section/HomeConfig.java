package org.AndrewElizabeth.teleportcommandsfabric.config.section;

public final class HomeConfig {
	public static final int MIN_PLAYER_MAXIMUM = 0;
	private boolean enabled = true;
	private int playerMaximum = 10;
	private boolean deleteInvalid = false;

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

	public void normalize() {
		setPlayerMaximum(playerMaximum);
	}
}
