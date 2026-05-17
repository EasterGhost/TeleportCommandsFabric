package org.AndrewElizabeth.teleportcommandsfabric.core.teleport;
 
import java.time.Duration;

public final class TeleportServiceSettings {
	public static final int FAST_PATH_THRESHOLD = 16;
	public static final int MAX_BATCH_SIZE_PER_TICK = 512;
	public static final int READY_ADMISSION_FIRST_TICK_LIMIT = 16;
	public static final int READY_ADMISSION_STEADY_TICK_LIMIT = 128;
	public static final long MAX_TELEPORT_BUDGET_NANOS = 1_500_000L;
	public static final int TIME_CHECK_INTERVAL = 16;
	public static final long PRELOAD_TIMEOUT_TICKS = 200L;
	public static final long PRELOAD_LEAD_TICKS = 2L;
	public static final Duration COOLDOWN_CLEANUP_DELAY = Duration.ofMinutes(10);
	public static final int PRELOAD_RADIUS_CHUNKS = 1;
	public static final int SAFETY_WORKER_THREADS = 8;
	public static final int SAFETY_BATCH_SIZE = 16;

	private TeleportServiceSettings() {
	}
}
