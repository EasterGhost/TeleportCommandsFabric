package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.TicketType;

public final class WildTicketTypes {
	private static TicketType wild;

	private WildTicketTypes() {
	}

	public static synchronized void initialize() {
		if (wild != null) {
			return;
		}
		wild = Registry.register(BuiltInRegistries.TICKET_TYPE,
				Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "wild"),
				new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING));
	}

	static TicketType wild() {
		if (wild == null) {
			throw new IllegalStateException("Wild ticket type has not been initialized");
		}
		return wild;
	}
}
