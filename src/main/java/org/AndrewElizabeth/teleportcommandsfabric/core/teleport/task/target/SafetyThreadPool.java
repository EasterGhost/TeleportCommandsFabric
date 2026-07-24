package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.target;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.TeleportServiceSettings;
import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class SafetyThreadPool {
	private static final BlockPos WARMUP_BASE_POS = BlockPos.ZERO;
	private static final BlockPos WARMUP_SAFE_OFFSET = new BlockPos(3, -3, 3);
	private static final int WARMUP_ITERATIONS_PER_THREAD = 250;
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

		int threads = TeleportServiceSettings.SAFETY_WORKER_THREADS;
		CyclicBarrier barrier = new CyclicBarrier(threads);
		TeleportSafety.BlockStateReader reader = createWarmupReader(WARMUP_BASE_POS);
		for (int i = 0; i < threads; i++) {
			CompletableFuture.runAsync(() -> {
				try {
					barrier.await(5, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				} catch (Exception ignored) {
				}

				BlockPos expectedSafePos = WARMUP_BASE_POS.offset(WARMUP_SAFE_OFFSET);
				for (int j = 0; j < WARMUP_ITERATIONS_PER_THREAD; j++) {
					if (Thread.currentThread().isInterrupted()) {
						return;
					}
					BlockPos safePos = TeleportSafety.getSafeBlockPos(WARMUP_BASE_POS, level, reader)
							.orElseThrow(() -> new IllegalStateException(
									"Synthetic teleport safety warmup found no safe position"));
					if (!safePos.equals(expectedSafePos)) {
						throw new IllegalStateException(
								"Synthetic teleport safety warmup returned an unexpected position: " + safePos);
					}
				}
			}, safetyExecutor).exceptionally(throwable -> {
				ModConstants.LOGGER.warn("Failed to warmup teleport safety worker", throwable);
				return null;
			});
		}
	}

	static TeleportSafety.BlockStateReader createWarmupReader(BlockPos basePos) {
		BlockPos waterPos = basePos.below();
		BlockPos witherRosePos = basePos;
		BlockPos lavaPos = basePos.offset(-2, -1, 0);
		BlockPos openFenceGatePos = basePos.offset(2, -1, 0);
		BlockPos safeSupportPos = basePos.offset(WARMUP_SAFE_OFFSET).below();
		BlockState openFenceGate = Blocks.OAK_FENCE_GATE.defaultBlockState()
				.setValue(FenceGateBlock.OPEN, true);

		return pos -> {
			if (pos.equals(waterPos)) {
				return Blocks.WATER.defaultBlockState();
			}
			if (pos.equals(witherRosePos)) {
				return Blocks.WITHER_ROSE.defaultBlockState();
			}
			if (pos.equals(lavaPos)) {
				return Blocks.LAVA.defaultBlockState();
			}
			if (pos.equals(openFenceGatePos)) {
				return openFenceGate;
			}
			if (pos.equals(safeSupportPos)) {
				return Blocks.STONE.defaultBlockState();
			}
			return Blocks.AIR.defaultBlockState();
		};
	}

	public void shutdown() {
		safetyExecutor.shutdownNow();
	}

	public ExecutorService getExecutor() {
		return safetyExecutor;
	}
}
