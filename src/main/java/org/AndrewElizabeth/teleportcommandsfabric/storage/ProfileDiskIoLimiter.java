package org.AndrewElizabeth.teleportcommandsfabric.storage;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

public final class ProfileDiskIoLimiter {
	private static final int MAX_CONCURRENT_DISK_OPERATIONS = 64;
	private static final Semaphore DISK_IO_PERMITS = new Semaphore(MAX_CONCURRENT_DISK_OPERATIONS);

	private ProfileDiskIoLimiter() {
	}

	public static <T> T run(Callable<T> operation) throws IOException {
		try {
			DISK_IO_PERMITS.acquire();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while waiting for profile disk IO permit", exception);
		}

		try {
			return operation.call();
		} catch (IOException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new IOException("Failed to run profile disk IO operation", exception);
		} finally {
			DISK_IO_PERMITS.release();
		}
	}
}
