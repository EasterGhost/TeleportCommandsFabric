package org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.client;

import org.AndrewElizabeth.teleportcommandsfabric.integration.xaero.network.protocol.XaeroSyncEntry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class XaeroWaypointCommandHelperTest {
	private static final Method BUILD_TAGGED_TELEPORT_COMMAND = taggedTeleportMethod();
	private static final Method BUILD_HIDE_COMMAND = hideCommandMethod();

	@Test
	void taggedWaypointNamesProduceTeleportCommands() {
		assertEquals("home Base", buildTaggedTeleportCommand("TPC-H Base"));
		assertEquals("warp Spawn", buildTaggedTeleportCommand("TPC-W Spawn"));
		assertEquals("home \"Main Base\"", buildTaggedTeleportCommand("TPC-H Main Base"));
	}

	@Test
	void untaggedWaypointNamesDoNotProduceTeleportCommands() {
		assertNull(buildTaggedTeleportCommand("Base"));
		assertNull(buildTaggedTeleportCommand("Spawn"));
	}

	@Test
	void hideCommandsAlsoRequireTeleportTags() {
		assertEquals("teleportcommandsfabric:maphome Base false", buildHideCommand("TPC-H Base"));
		assertEquals("teleportcommandsfabric:mapwarp Spawn false", buildHideCommand("TPC-W Spawn"));
		assertEquals("teleportcommandsfabric:maphome \"Main Base\" false", buildHideCommand("TPC-H Main Base"));
		assertNull(buildHideCommand("Base"));
		assertNull(buildHideCommand("Spawn"));
	}

	@Test
	void syncedWaypointCreationAddsTeleportTags() {
		assertEquals("TPC-H Base", taggedWaypointName());
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static String taggedWaypointName() {
		try {
			Class<?> entryTypeClass = Class.forName(XaeroCompat.class.getName() + "$EntryType");
			Object homeType = Enum.valueOf((Class<Enum>) entryTypeClass.asSubclass(Enum.class), "HOME");
			Method method = XaeroCompat.class.getDeclaredMethod("toTaggedWaypoints", List.class, entryTypeClass);
			method.setAccessible(true);
			List<?> waypoints = (List<?>) method.invoke(null,
					List.of(new XaeroSyncEntry("Base", "minecraft:overworld", 1, 64, 2)), homeType);
			Object waypoint = waypoints.getFirst();
			Method getName = waypoint.getClass().getMethod("getName");
			return (String) getName.invoke(waypoint);
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError("Unable to inspect Xaero waypoint creation", exception);
		}
	}

	private static String buildTaggedTeleportCommand(String name) {
		return invokeStringHelper(BUILD_TAGGED_TELEPORT_COMMAND, name, "tagged teleport command helper");
	}

	private static String buildHideCommand(String name) {
		return invokeStringHelper(BUILD_HIDE_COMMAND, name, "hide command helper");
	}

	private static String invokeStringHelper(Method method, String name, String description) {
		try {
			return (String) method.invoke(null, name);
		} catch (IllegalAccessException exception) {
			throw new AssertionError("Unable to access " + description, exception);
		} catch (InvocationTargetException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw new AssertionError(description + " failed", cause);
		}
	}

	private static Method taggedTeleportMethod() {
		try {
			Method method = XaeroWaypointCommandHelper.class.getDeclaredMethod("buildTaggedTeleportCommand",
					String.class);
			method.setAccessible(true);
			return method;
		} catch (NoSuchMethodException exception) {
			throw new AssertionError("Tagged teleport command helper method is missing", exception);
		}
	}

	private static Method hideCommandMethod() {
		try {
			Method method = XaeroWaypointCommandHelper.class.getDeclaredMethod("buildHideCommand", String.class);
			method.setAccessible(true);
			return method;
		} catch (NoSuchMethodException exception) {
			throw new AssertionError("Hide command helper method is missing", exception);
		}
	}
}
