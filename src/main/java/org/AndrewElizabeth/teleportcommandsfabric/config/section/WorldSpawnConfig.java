package org.AndrewElizabeth.teleportcommandsfabric.config.section;

public final class WorldSpawnConfig {
	private static final String DEFAULT_WORLD_ID = "minecraft:overworld";
	private boolean enabled = true;
	private String world_id = DEFAULT_WORLD_ID;

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

	public void normalize() {
		if (world_id == null || world_id.isBlank()) {
			world_id = DEFAULT_WORLD_ID;
		}
	}
}
