package org.AndrewElizabeth.teleportcommandsfabric.modules.wild;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

final class WildNodeFactory {
	private WildNodeFactory() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("wild")
				.requires(source -> source.getPlayer() != null)
				.executes(context -> WildHandler.execute(context.getSource().getPlayerOrException()));
	}
}
