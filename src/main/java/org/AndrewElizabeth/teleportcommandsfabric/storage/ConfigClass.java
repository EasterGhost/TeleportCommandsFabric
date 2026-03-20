package org.AndrewElizabeth.teleportcommandsfabric.storage;

import org.AndrewElizabeth.teleportcommandsfabric.Constants;

public class ConfigClass {
	private final int version = Constants.CONFIG_VERSION;
	public Teleporting teleporting = new Teleporting();
	public Back back = new Back();
	public Home home = new Home();
	public Tpa tpa = new Tpa();
	public Warp warp = new Warp();
	public WorldSpawn worldSpawn = new WorldSpawn();
	public Rtp rtp = new Rtp();
	public Xaero xaero = new Xaero();
	public Storage storage = new Storage();

	public int getVersion() {
		return version;
	}

	public Teleporting getTeleporting() {
		return teleporting;
	}

	public Back getBack() {
		return back;
	}

	public Home getHome() {
		return home;
	}

	public Tpa getTpa() {
		return tpa;
	}

	public Warp getWarp() {
		return warp;
	}

	public WorldSpawn getWorldSpawn() {
		return worldSpawn;
	}

	public Rtp getRtp() {
		return rtp;
	}

	public Xaero getXaero() {
		return xaero;
	}

	public static final class Teleporting {
		private int delay = 0;
		private int cooldown = 3;
		private boolean preloadEnabled = false;
		private int preloadRadiusChunks = 1;

		public int getDelay() {
			return delay;
		}

		public void setDelay(int delay) {
			this.delay = delay;
		}

		public int getCooldown() {
			return cooldown;
		}

		public void setCooldown(int cooldown) {
			this.cooldown = cooldown;
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
			this.preloadRadiusChunks = preloadRadiusChunks;
		}
	}

	public final class Back {
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
	}

	public final class Home {
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
			this.playerMaximum = playerMaximum;
		}

		public boolean isDeleteInvalid() {
			return deleteInvalid;
		}

		public void setDeleteInvalid(boolean deleteInvalid) {
			this.deleteInvalid = deleteInvalid;
		}
	}

	public final class Tpa {
		private boolean enabled = true;
		private int requestExpireTime = 120;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getRequestExpireTime() {
			return requestExpireTime;
		}

		public void setRequestExpireTime(int requestExpireTime) {
			this.requestExpireTime = requestExpireTime;
		}
	}

	public final class Warp {
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
			this.maximum = maximum;
		}

		public boolean isDeleteInvalid() {
			return deleteInvalid;
		}

		public void setDeleteInvalid(boolean deleteInvalid) {
			this.deleteInvalid = deleteInvalid;
		}
	}

	public final class Rtp {
		public static final int MIN_RADIUS = 1;
		public static final int MAX_RADIUS = 128;
		private boolean enabled = true;
		private int radius = 32;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getRadius() {
			return Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));
		}

		public void setRadius(int radius) {
			this.radius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));
		}
	}

	public final class WorldSpawn {
		private boolean enabled = true;
		private String world_id = "minecraft:overworld";

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getWorld_id() {
			return world_id;
		}

		public void setWorld_id(String world_id) {
			this.world_id = world_id;
		}
	}

	public final class Xaero {
		private boolean enabled = true;
		private int syncIntervalSeconds = 10;
		private boolean persistWaypointSets = true;
		private String warpSetName = "Default";
		private String homeSetName = "Default";

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getSyncIntervalSeconds() {
			return syncIntervalSeconds;
		}

		public void setSyncIntervalSeconds(int syncIntervalSeconds) {
			this.syncIntervalSeconds = syncIntervalSeconds;
		}

		public boolean isPersistWaypointSets() {
			return persistWaypointSets;
		}

		public void setPersistWaypointSets(boolean persistWaypointSets) {
			this.persistWaypointSets = persistWaypointSets;
		}

		public String getWarpSetName() {
			return warpSetName;
		}

		public void setWarpSetName(String warpSetName) {
			this.warpSetName = warpSetName;
		}

		public String getHomeSetName() {
			return homeSetName;
		}

		public void setHomeSetName(String homeSetName) {
			this.homeSetName = homeSetName;
		}
	}

	public static final class Storage {
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
	}
}
