package org.AndrewElizabeth.teleportcommandsfabric.config.section;

public final class WarpConfig {
	public static final int MIN_MAXIMUM = 0;
	private boolean enabled = true;
	private int maximum = 0;
	private boolean deleteInvalid = false;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getMaximum() {
		return maximum;
	}

	public void setMaximum(int maximum) {
		this.maximum = Math.max(MIN_MAXIMUM, maximum);
	}

	public boolean isDeleteInvalid() {
		return deleteInvalid;
	}

	public void setDeleteInvalid(boolean deleteInvalid) {
		this.deleteInvalid = deleteInvalid;
	}

	public void normalize() {
		setMaximum(maximum);
	}
}
