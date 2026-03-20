package org.AndrewElizabeth.teleportcommandsfabric.commands.common;

import java.util.Optional;

public final class VisibilityCommandSupport {
	@FunctionalInterface
	public interface VisibilityResolver<T> {
		Optional<T> resolve() throws Exception;
	}

	@FunctionalInterface
	public interface VisibilityChecker<T> {
		boolean isVisible(T target) throws Exception;
	}

	@FunctionalInterface
	public interface VisibilityUpdater<T> {
		void setVisible(T target, boolean visible) throws Exception;
	}

	@FunctionalInterface
	public interface VisibilityHook {
		void run() throws Exception;
	}

	private VisibilityCommandSupport() {
	}

	public static <T> void update(
			boolean visible,
			VisibilityResolver<T> resolver,
			VisibilityChecker<T> checker,
			VisibilityUpdater<T> updater,
			VisibilityHook onAlreadyVisible,
			VisibilityHook onChanged) throws Exception {
		Optional<T> optionalTarget = resolver.resolve();
		if (optionalTarget.isEmpty()) {
			return;
		}

		T target = optionalTarget.get();
		if (checker.isVisible(target) == visible) {
			onAlreadyVisible.run();
			return;
		}

		updater.setVisible(target, visible);
		onChanged.run();
	}
}
