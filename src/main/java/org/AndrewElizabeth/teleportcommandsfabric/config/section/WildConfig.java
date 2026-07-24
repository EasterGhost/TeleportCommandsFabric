package org.AndrewElizabeth.teleportcommandsfabric.config.section;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.wild.WildRequest;

public final class WildConfig {
	private boolean enabled = true;
	private int minRadius = 512;
	private int maxRadius = 4096;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getMinRadius() {
		return minRadius;
	}

	public void setMinRadius(int minRadius) {
		this.minRadius = Math.max(WildRequest.MIN_RADIUS, Math.min(maxRadius, minRadius));
	}

	public int getMaxRadius() {
		return maxRadius;
	}

	public void setMaxRadius(int maxRadius) {
		this.maxRadius = Math.max(WildRequest.MIN_RADIUS, Math.min(WildRequest.MAX_RADIUS, maxRadius));
		if (minRadius > this.maxRadius) {
			minRadius = this.maxRadius;
		}
	}

	public void normalize() {
		setMaxRadius(maxRadius);
		setMinRadius(minRadius);
	}
}
