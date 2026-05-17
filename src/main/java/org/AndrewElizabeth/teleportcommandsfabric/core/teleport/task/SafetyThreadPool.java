package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportServiceSettings;
import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class SafetyThreadPool {
	private final ExecutorService safetyExecutor;

	public SafetyThreadPool() {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				TeleportServiceSettings.SAFETY_WORKER_THREADS,
				TeleportServiceSettings.SAFETY_WORKER_THREADS,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(),
				runnable -> {
					Thread thread = new Thread(runnable, "TeleportSafetyWorker");
					thread.setDaemon(true);
					return thread;
				});
		executor.prestartAllCoreThreads();
		this.safetyExecutor = executor;
	}

	public void warmupSafety(MinecraftServer server) {
		ServerLevel level = server.getAllLevels().iterator().hasNext() ? server.getAllLevels().iterator().next() : null;
		if (level == null) {
			return;
		}
		BlockPos spawnPos = level.getRespawnData().pos();
		int threads = TeleportServiceSettings.SAFETY_WORKER_THREADS;
		int warmupIterationsPerThread = 250;
		CyclicBarrier barrier = new CyclicBarrier(threads);
		for (int i = 0; i < threads; i++) {
			CompletableFuture.runAsync(() -> {
				try {
					barrier.await(5, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (Exception ignored) {
				}
				for (int j = 0; j < warmupIterationsPerThread; j++) {
					if (TeleportSafety.getSafeBlockPos(spawnPos, level).isEmpty()) {
						ModConstants.LOGGER.warn("Teleport safety warmup check returned no safe position.");
						break;
					}
				}
			}, safetyExecutor).exceptionally(throwable -> {
				ModConstants.LOGGER.warn("Failed to warmup teleport safety worker", throwable);
				return null;
			});
		}
	}

	public void shutdown() {
		safetyExecutor.shutdownNow();
	}

	public ExecutorService getExecutor() {
		return safetyExecutor;
	}
}
