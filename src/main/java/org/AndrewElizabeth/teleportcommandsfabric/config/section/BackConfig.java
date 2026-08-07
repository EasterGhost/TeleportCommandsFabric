package org.AndrewElizabeth.teleportcommandsfabric.config.section;

public final class BackConfig {
	private boolean enabled = true;
	private boolean deleteAfterTeleport = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isDeleteAfterTeleport() {
		return deleteAfterTeleport;
	}

	public void setDeleteAfterTeleport(boolean deleteAfterTeleport) {
		this.deleteAfterTeleport = deleteAfterTeleport;
	}

	public void normalize() {
	}
}
