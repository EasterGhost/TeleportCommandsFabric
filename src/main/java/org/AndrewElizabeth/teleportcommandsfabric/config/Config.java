package org.AndrewElizabeth.teleportcommandsfabric.config;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.config.section.*;

public class Config {
	private final int version = ModConstants.CONFIG_VERSION;
	private boolean debugEnabled = false;
	public TeleportingConfig teleporting = new TeleportingConfig();
	public BackConfig back = new BackConfig();
	public HomeConfig home = new HomeConfig();
	public TpaConfig tpa = new TpaConfig();
	public WarpConfig warp = new WarpConfig();
	public WorldSpawnConfig worldSpawn = new WorldSpawnConfig();
	public RtpConfig rtp = new RtpConfig();
	public XaeroConfig xaero = new XaeroConfig();
	public StorageConfig storage = new StorageConfig();

	public int getVersion() {
		return version;
	}

	public boolean isDebugEnabled() {
		return debugEnabled;
	}

	public void setDebugEnabled(boolean debugEnabled) {
		this.debugEnabled = debugEnabled;
	}

	public TeleportingConfig getTeleporting() {
		return teleporting;
	}

	public BackConfig getBack() {
		return back;
	}

	public HomeConfig getHome() {
		return home;
	}

	public TpaConfig getTpa() {
		return tpa;
	}

	public WarpConfig getWarp() {
		return warp;
	}

	public WorldSpawnConfig getWorldSpawn() {
		return worldSpawn;
	}

	public RtpConfig getRtp() {
		return rtp;
	}

	public XaeroConfig getXaero() {
		return xaero;
	}

	public StorageConfig getStorage() {
		return storage;
	}

	public Config normalize() {
		if (teleporting == null) {
			teleporting = new TeleportingConfig();
		}
		if (back == null) {
			back = new BackConfig();
		}
		if (home == null) {
			home = new HomeConfig();
		}
		if (tpa == null) {
			tpa = new TpaConfig();
		}
		if (warp == null) {
			warp = new WarpConfig();
		}
		if (worldSpawn == null) {
			worldSpawn = new WorldSpawnConfig();
		}
		if (rtp == null) {
			rtp = new RtpConfig();
		}
		if (xaero == null) {
			xaero = new XaeroConfig();
		}
		if (storage == null) {
			storage = new StorageConfig();
		}

		teleporting.normalize();
		back.normalize();
		home.normalize();
		tpa.normalize();
		warp.normalize();
		rtp.normalize();
		worldSpawn.normalize();
		xaero.normalize();
		storage.normalize();
		return this;
	}
}
