package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types;

public record TeleportOptions(int delayTicks, long cooldownMillis, boolean safetyEnabled, boolean recordPrevious) {
	public static final TeleportOptions DEFAULT = new TeleportOptions(0, 0, true, true);

	public static Builder builder() {
		return new Builder();
	}

	public long effectiveCooldownMillis() {
		return cooldownMillis;
	}

	public static class Builder {
		private int delayTicks = 0;
		private long cooldownMillis = 0;
		private boolean safetyEnabled = true;
		private boolean recordPrevious = true;

		public Builder delayTicks(int delayTicks) {
			this.delayTicks = delayTicks;
			return this;
		}

		public Builder cooldownMillis(long cooldownMillis) {
			this.cooldownMillis = cooldownMillis;
			return this;
		}

		public Builder safetyEnabled(boolean safetyEnabled) {
			this.safetyEnabled = safetyEnabled;
			return this;
		}

		public Builder recordPrevious(boolean recordPrevious) {
			this.recordPrevious = recordPrevious;
			return this;
		}

		public TeleportOptions build() {
			return new TeleportOptions(delayTicks, cooldownMillis, safetyEnabled, recordPrevious);
		}
	}
}
