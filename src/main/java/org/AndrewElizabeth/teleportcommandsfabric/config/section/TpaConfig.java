package org.AndrewElizabeth.teleportcommandsfabric.config.section;

import java.time.Duration;

public final class TpaConfig {
	public static final Duration MIN_REQUEST_EXPIRE_TIME = Duration.ZERO;
	private boolean enabled = true;
	private Duration requestExpireTime = Duration.ofSeconds(120);

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Duration getRequestExpireTime() {
		return requestExpireTime;
	}

	public void setRequestExpireTime(Duration requestExpireTime) {
		if (requestExpireTime == null || requestExpireTime.compareTo(MIN_REQUEST_EXPIRE_TIME) < 0) {
			this.requestExpireTime = MIN_REQUEST_EXPIRE_TIME;
		} else {
			this.requestExpireTime = requestExpireTime;
		}
	}

	public void normalize() {
		if (requestExpireTime == null || requestExpireTime.compareTo(MIN_REQUEST_EXPIRE_TIME) < 0) {
			requestExpireTime = MIN_REQUEST_EXPIRE_TIME;
		}
	}
}
