package org.AndrewElizabeth.teleportcommandsfabric.modules.tpa;

import com.mojang.brigadier.CommandDispatcher;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.tpa.Tpa;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class TpaCommand {
	private TpaCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(TpaNodeFactory.buildRequestNode("tpa", Tpa.Type.TPA));
		dispatcher.register(TpaNodeFactory.buildRequestNode("tpahere", Tpa.Type.TPAHERE));
		dispatcher.register(TpaNodeFactory.buildResponseNode("tpaaccept", true));
		dispatcher.register(TpaNodeFactory.buildResponseNode("tpadeny", false));
	}

	public static void sendExpired(MinecraftServer server, Tpa.Session session) {
		if (server == null || session == null) {
			return;
		}
		ServerPlayer sender = server.getPlayerList().getPlayer(session.sender());
		ServerPlayer target = server.getPlayerList().getPlayer(session.target());
		TpaMessages.sendExpired(sender, target, session.type());
	}
}
