package org.AndrewElizabeth.teleportcommandsfabric.modules.common;

public final class TargetTeleportSafety {
	private TargetTeleportSafety() {
	}

	public static boolean resolveEnabled(boolean defaultSafetyCheck, Boolean safetyDisabledOverride) {
		return safetyDisabledOverride == null ? defaultSafetyCheck : !safetyDisabledOverride;
	}
}
