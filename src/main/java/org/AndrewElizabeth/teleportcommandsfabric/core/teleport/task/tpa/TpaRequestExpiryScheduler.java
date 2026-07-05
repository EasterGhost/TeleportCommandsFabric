package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.tpa;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class TpaRequestExpiryScheduler {
	private final ScheduledExecutorService scheduler;
	private final Map<UUID, ScheduledFuture<?>> tasks = new HashMap<>(128);
	private final ConcurrentLinkedQueue<UUID> expiredSessionIds = new ConcurrentLinkedQueue<>();

	public TpaRequestExpiryScheduler() {
		this(createScheduler());
	}

	TpaRequestExpiryScheduler(ScheduledExecutorService scheduler) {
		this.scheduler = scheduler;
	}

	public void schedule(UUID sessionId, Duration expiry) {
		if (sessionId == null) {
			return;
		}
		cancel(sessionId);
		long delayMillis = Math.max(0L, expiry == null ? 0L : expiry.toMillis());
		ScheduledFuture<?> future = scheduler.schedule(() -> expiredSessionIds.add(sessionId),
				delayMillis, TimeUnit.MILLISECONDS);
		tasks.put(sessionId, future);
	}

	public void cancel(UUID sessionId) {
		if (sessionId == null) {
			return;
		}
		ScheduledFuture<?> future = tasks.remove(sessionId);
		if (future != null) {
			future.cancel(false);
		}
	}

	public void cancelAll() {
		for (ScheduledFuture<?> future : tasks.values()) {
			future.cancel(false);
		}
		tasks.clear();
		expiredSessionIds.clear();
	}

	public void drainExpired(Consumer<UUID> consumer) {
		UUID sessionId;
		while ((sessionId = expiredSessionIds.poll()) != null) {
			consumer.accept(sessionId);
		}
	}

	public void shutdown() {
		cancelAll();
		scheduler.shutdownNow();
	}

	private static ScheduledExecutorService createScheduler() {
		return Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable, "teleportcommands-tpa-expiration");
			thread.setDaemon(true);
			return thread;
		});
	}
}
