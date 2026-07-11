package org.AndrewElizabeth.teleportcommandsfabric;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
 
public class ModConstants {
	public static final String MOD_ID = "teleport_commands_fabric";
	public static final String ASSETS_ID = "teleport_commands_fabric";
	public static final String MOD_NAME = "Teleport Commands Fabric";
	public static final String VERSION = "2.3.1";
	public static final int CONFIG_VERSION = 3;
	public static final int STORAGE_VERSION = 1;
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
	public static final Duration SYNC_INTERVAL = Duration.ofSeconds(1);
}
