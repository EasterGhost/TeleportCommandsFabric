package org.AndrewElizabeth.teleportcommandsfabric.config.section;

public final class RtpConfig {
	public static final int MIN_RADIUS = 1;
	public static final int MAX_RADIUS = 128;
	public static final int MIN_MIN_RADIUS = 0;
	private boolean enabled = true;
	private int maxRadius = 32;
	private int minRadius = 4;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getMaxRadius() {
		return maxRadius;
	}

	public void setMaxRadius(int maxRadius) {
		this.maxRadius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, maxRadius));
		if (minRadius > this.maxRadius) {
			minRadius = this.maxRadius;
		}
	}

	public int getMinRadius() {
		return minRadius;
	}

	public void setMinRadius(int minRadius) {
		this.minRadius = Math.max(MIN_MIN_RADIUS, Math.min(maxRadius, minRadius));
	}

	public void normalize() {
		setMaxRadius(maxRadius);
		setMinRadius(minRadius);
	}
}
