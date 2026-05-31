package org.AndrewElizabeth.teleportcommandsfabric.testsupport;

import org.junit.jupiter.api.DynamicTest;

public final class ScenarioTestSupport {
	private static final String SECTION_SEPARATOR = "================================================================================";

	private ScenarioTestSupport() {
	}

	public static DynamicTest scenario(String name, String purpose, ScenarioBody body) {
		return scenario(name, purpose, "", body);
	}

	public static DynamicTest scenario(String name, String purpose, String params, ScenarioBody body) {
		return DynamicTest.dynamicTest(name, () -> runScenario(name, purpose, params, body));
	}

	public static void debug(String key, String value) {
		System.out.println("  " + key + ": " + value);
	}

	public static void debugBlock(String title, String text) {
		System.out.println("  " + title + ":");
		String[] lines = text.split("\\R", -1);
		for (String line : lines) {
			System.out.println("    " + line);
		}
	}

	public static String formatNumber(long value) {
		return String.format("%,d", value);
	}

	public static String formatDecimal(double value) {
		return String.format("%,.2f", value);
	}

	public static String formatLongArray(long[] values) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append(formatNumber(values[i]));
		}
		return builder.toString();
	}

	public static String formatIntArray(int[] values) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append(formatNumber(values[i]));
		}
		return builder.toString();
	}

	private static void runScenario(String name, String purpose, String params, ScenarioBody body) throws Exception {
		long scenarioStart = System.nanoTime();
		System.out.println();
		System.out.println(SECTION_SEPARATOR);
		System.out.println("SCENARIO START: " + name);
		System.out.println("  PURPOSE: " + purpose);
		if (!params.isBlank()) {
			System.out.println("  PARAMS: " + params);
		}
		try {
			body.run();
			System.out.println("SCENARIO PASS: " + name
					+ " elapsedNanos=" + formatNumber(System.nanoTime() - scenarioStart));
		} catch (Throwable throwable) {
			System.err.println("SCENARIO FAIL: " + name);
			throw throwable;
		}
	}

	@FunctionalInterface
	public interface ScenarioBody {
		void run() throws Exception;
	}
}
