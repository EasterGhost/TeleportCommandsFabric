package org.AndrewElizabeth.teleportcommandsfabric.utils;

public final class CommandHelper {
	private CommandHelper() {
	}

	public static String quoteCommandArgument(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}
}
