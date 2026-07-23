package org.AndrewElizabeth.teleportcommandsfabric.config;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerConcurrencyTest {
	@TempDir
	Path tempDir;

	@Test
	void reloadRejectsMutationAndCoalescesConcurrentRequests() throws Exception {
		Path previousConfigDir = TeleportCommands.CONFIG_DIR;
		Object previousConfigFile = field("CONFIG_FILE").get(null);
		Object previousConfig = field("CONFIG").get(null);
		Object previousExecutor = field("IO_EXECUTOR").get(null);
		Object previousReloadFuture = field("RELOAD_FUTURE").get(null);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		CountDownLatch blockerStarted = new CountDownLatch(1);
		CountDownLatch releaseBlocker = new CountDownLatch(1);

		try {
			TeleportCommands.CONFIG_DIR = tempDir;
			field("CONFIG_FILE").set(null, tempDir.resolve("teleport_commands.json"));
			field("CONFIG").set(null, new Config());
			field("IO_EXECUTOR").set(null, executor);
			field("RELOAD_FUTURE").set(null, null);
			executor.submit(() -> {
				blockerStarted.countDown();
				try {
					releaseBlocker.await();
				} catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
				}
			});
			assertTrue(blockerStarted.await(5, TimeUnit.SECONDS));

			CompletableFuture<Void> firstReload = ConfigManager.reload();
			assertSame(firstReload, ConfigManager.reload());
			assertThrows(IllegalStateException.class, () -> ConfigManager.mutate(config -> {
			}));

			releaseBlocker.countDown();
			firstReload.join();
			assertDoesNotThrow(() -> ConfigManager.mutate(config -> {
			}));
			ConfigManager.shutdown().join();
		} finally {
			releaseBlocker.countDown();
			executor.shutdownNow();
			TeleportCommands.CONFIG_DIR = previousConfigDir;
			field("CONFIG_FILE").set(null, previousConfigFile);
			field("CONFIG").set(null, previousConfig);
			field("IO_EXECUTOR").set(null, previousExecutor);
			field("RELOAD_FUTURE").set(null, previousReloadFuture);
		}
	}

	@Test
	void reloadSubmissionFailureClearsReloadGate() throws Exception {
		Object previousConfigFile = field("CONFIG_FILE").get(null);
		Object previousConfig = field("CONFIG").get(null);
		Object previousExecutor = field("IO_EXECUTOR").get(null);
		Object previousReloadFuture = field("RELOAD_FUTURE").get(null);

		try {
			field("CONFIG_FILE").set(null, tempDir.resolve("teleport_commands.json"));
			field("CONFIG").set(null, new Config());
			field("IO_EXECUTOR").set(null, new RejectingExecutorService());
			field("RELOAD_FUTURE").set(null, null);

			CompletableFuture<Void> reload = ConfigManager.reload();
			assertThrows(CompletionException.class, reload::join);
			field("IO_EXECUTOR").set(null, null);
			assertDoesNotThrow(() -> ConfigManager.mutate(config -> {
			}));
		} finally {
			ExecutorService currentExecutor = (ExecutorService) field("IO_EXECUTOR").get(null);
			if (currentExecutor != null) {
				currentExecutor.shutdownNow();
			}
			field("CONFIG_FILE").set(null, previousConfigFile);
			field("CONFIG").set(null, previousConfig);
			field("IO_EXECUTOR").set(null, previousExecutor);
			field("RELOAD_FUTURE").set(null, previousReloadFuture);
		}
	}

	private static Field field(String name) throws NoSuchFieldException {
		Field field = ConfigManager.class.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	private static final class RejectingExecutorService extends AbstractExecutorService {
		private boolean shutdown;

		@Override
		public void shutdown() {
			shutdown = true;
		}

		@Override
		public List<Runnable> shutdownNow() {
			shutdown = true;
			return List.of();
		}

		@Override
		public boolean isShutdown() {
			return shutdown;
		}

		@Override
		public boolean isTerminated() {
			return shutdown;
		}

		@Override
		public boolean awaitTermination(long timeout, TimeUnit unit) {
			return shutdown;
		}

		@Override
		public void execute(Runnable command) {
			throw new RejectedExecutionException("Rejected for test");
		}
	}
}
