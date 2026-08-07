package org.AndrewElizabeth.teleportcommandsfabric.storage;

import java.io.IOException;
import java.util.function.Predicate;

public final class ProfileLifecycleSupport {
	private ProfileLifecycleSupport() {
	}

	public static <T> boolean flush(T profile, Predicate<T> isEmpty, IoAction deleteEmptyProfile, IoConsumer<T> saveProfile)
			throws IOException {
		if (isEmpty.test(profile)) {
			deleteEmptyProfile.run();
			return false;
		}

		saveProfile.accept(profile);
		return true;
	}

	@FunctionalInterface
	public interface IoAction {
		void run() throws IOException;
	}

	@FunctionalInterface
	public interface IoConsumer<T> {
		void accept(T value) throws IOException;
	}
}
