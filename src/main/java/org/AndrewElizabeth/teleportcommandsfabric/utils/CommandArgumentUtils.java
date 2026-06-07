package org.AndrewElizabeth.teleportcommandsfabric.utils;

public final class CommandArgumentUtils {
	private static final String SAFE_UNQUOTED_ARGUMENT_PATTERN = "[A-Za-z0-9_+.-]+";

	private CommandArgumentUtils() {
	}

	public static String quote(String value) {
		String safeValue = value == null ? "" : value;
		if (safeValue.matches(SAFE_UNQUOTED_ARGUMENT_PATTERN)) {
			return safeValue;
		}
		return "\"" + safeValue.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}
}
