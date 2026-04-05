package org.AndrewElizabeth.teleportcommandsfabric.core.command;

public final class CommandArgumentSupport {
	private CommandArgumentSupport() {
	}

	public static String quoteCommandArgument(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}
}
