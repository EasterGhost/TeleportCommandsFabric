package org.AndrewElizabeth.teleportcommandsfabric.storage;

import java.io.IOException;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public final class ProfileLifecycleSupport {
	private ProfileLifecycleSupport() {
	}

	public static boolean prepareLoaded(boolean deleteInvalid, BooleanSupplier refreshState,
			BooleanSupplier removeInvalid, Runnable rebuildIndex) {
		boolean changed = refreshState.getAsBoolean();
		if (deleteInvalid) {
			changed |= removeInvalid.getAsBoolean();
		}
		rebuildIndex.run();
		return changed;
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
