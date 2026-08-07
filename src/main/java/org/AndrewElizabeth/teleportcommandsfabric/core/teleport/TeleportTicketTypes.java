package org.AndrewElizabeth.teleportcommandsfabric.core.teleport;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.TicketType;

public final class TeleportTicketTypes {
	private static volatile TicketType targetPreload;
	private static volatile TicketType wild;

	private TeleportTicketTypes() {
	}

	public static synchronized void initialize() {
		if (targetPreload != null) {
			return;
		}
		targetPreload = register("target_preload");
		wild = register("wild");
	}

	public static TicketType targetPreload() {
		return requireInitialized(targetPreload);
	}

	public static TicketType wild() {
		return requireInitialized(wild);
	}

	private static TicketType register(String path) {
		return Registry.register(BuiltInRegistries.TICKET_TYPE,
				Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, path),
				new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING));
	}

	private static TicketType requireInitialized(TicketType ticketType) {
		if (ticketType == null) {
			throw new IllegalStateException("Teleport ticket types have not been initialized");
		}
		return ticketType;
	}
}
