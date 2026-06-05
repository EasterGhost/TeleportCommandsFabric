package org.AndrewElizabeth.teleportcommandsfabric.storage;

import java.util.concurrent.CompletionException;

public final class StorageFutures {
	private StorageFutures() {
	}

	public static Throwable unwrapCompletionException(Throwable throwable) {
		if (throwable instanceof CompletionException && throwable.getCause() != null) {
			return throwable.getCause();
		}
		return throwable;
	}
}
