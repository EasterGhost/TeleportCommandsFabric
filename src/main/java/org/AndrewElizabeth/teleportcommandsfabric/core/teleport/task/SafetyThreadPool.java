package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportServiceSettings;
import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.Iterator;
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
		Iterator<ServerLevel> levels = server.getAllLevels().iterator();
		ServerLevel level = levels.hasNext() ? levels.next() : null;
		if (level == null) {
			return;
		}
		BlockPos spawnPos = level.getRespawnData().pos();
		ChunkPos chunkPos = new ChunkPos(spawnPos.getX() >> 4, spawnPos.getZ() >> 4);
		int ticketRadius = 1;
		level.getChunkSource().addTicketWithRadius(TicketType.UNKNOWN, chunkPos, ticketRadius);

		int threads = TeleportServiceSettings.SAFETY_WORKER_THREADS;
		int warmupIterationsPerThread = 250;
		CyclicBarrier barrier = new CyclicBarrier(threads);
		List<CompletableFuture<Void>> futures = new ArrayList<>(threads);
		for (int i = 0; i < threads; i++) {
			futures.add(CompletableFuture.runAsync(() -> {
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
			}));
		}

		CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
				.whenComplete((ignored, throwable) -> server.execute(() ->
						level.getChunkSource().removeTicketWithRadius(TicketType.UNKNOWN, chunkPos, ticketRadius)));
	}

	public void shutdown() {
		safetyExecutor.shutdownNow();
	}

	public ExecutorService getExecutor() {
		return safetyExecutor;
	}
}
