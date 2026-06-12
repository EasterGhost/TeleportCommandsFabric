package org.AndrewElizabeth.teleportcommandsfabric.client.xaero;

import org.AndrewElizabeth.teleportcommandsfabric.network.protocol.xaero.XaeroSyncEntry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class XaeroWaypointCommandHelperTest {
	private static final Method BUILD_TAGGED_TELEPORT_COMMAND = taggedTeleportMethod();

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
	void customSetWaypointCreationKeepsNamesUntagged() {
		assertEquals("Base", createdWaypointName(false));
		assertEquals("TPC-H Base", createdWaypointName(true));
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static String createdWaypointName(boolean tagged) {
		try {
			Class<?> entryTypeClass = Class.forName(XaeroCompat.class.getName() + "$EntryType");
			Object homeType = Enum.valueOf((Class<Enum>) entryTypeClass.asSubclass(Enum.class), "HOME");
			Method method = XaeroCompat.class.getDeclaredMethod("createWaypoints", List.class, entryTypeClass,
					boolean.class);
			method.setAccessible(true);
			List<?> waypoints = (List<?>) method.invoke(null,
					List.of(new XaeroSyncEntry("Base", "minecraft:overworld", 1, 64, 2)), homeType, tagged);
			Object waypoint = waypoints.getFirst();
			Method getName = waypoint.getClass().getMethod("getName");
			return (String) getName.invoke(waypoint);
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError("Unable to inspect Xaero waypoint creation", exception);
		}
	}

	private static String buildTaggedTeleportCommand(String name) {
		try {
			return (String) BUILD_TAGGED_TELEPORT_COMMAND.invoke(null, name);
		} catch (IllegalAccessException exception) {
			throw new AssertionError("Unable to access tagged teleport command helper", exception);
		} catch (InvocationTargetException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw new AssertionError("Tagged teleport command helper failed", cause);
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
}
