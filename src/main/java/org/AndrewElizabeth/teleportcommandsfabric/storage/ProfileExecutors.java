package org.AndrewElizabeth.teleportcommandsfabric.storage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class ProfileExecutors {
	private ProfileExecutors() {
	}

	public static ExecutorService createVirtualIoExecutor(String threadNamePrefix) {
		return Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
				.name(threadNamePrefix, 0)
				.factory());
	}

	public static ScheduledExecutorService createSingleThreadScheduler(String threadName) {
		return Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, threadName);
			thread.setDaemon(true);
			return thread;
		});
	}
}
