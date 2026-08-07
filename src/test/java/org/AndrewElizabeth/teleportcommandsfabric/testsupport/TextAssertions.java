package org.AndrewElizabeth.teleportcommandsfabric.testsupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TextAssertions {
	private TextAssertions() {
	}

	public static void assertContains(String text, String expected, String message) {
		assertTrue(text.contains(expected),
				() -> message + "\nexpected to contain:\n" + expected + "\nactual:\n" + text);
	}

	public static void assertNotContains(String text, String unexpected, String message) {
		assertFalse(text.contains(unexpected),
				() -> message + "\nunexpected:\n" + unexpected + "\nactual:\n" + text);
	}
}
