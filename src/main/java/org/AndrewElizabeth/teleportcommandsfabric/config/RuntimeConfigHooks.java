package org.AndrewElizabeth.teleportcommandsfabric.config;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RuntimeConfigHooks {
	private static final List<Runnable> HOOKS = new CopyOnWriteArrayList<>();

	private RuntimeConfigHooks() {
	}

	public static void register(Runnable hook) {
		if (hook == null) {
			throw new IllegalArgumentException("hook must not be null");
		}
		HOOKS.add(hook);
	}

	static void applyAll() {
		for (Runnable hook : HOOKS) {
			try {
				hook.run();
			} catch (RuntimeException exception) {
				ModConstants.LOGGER.error("Runtime config hook failed.", exception);
			}
		}
	}
}
