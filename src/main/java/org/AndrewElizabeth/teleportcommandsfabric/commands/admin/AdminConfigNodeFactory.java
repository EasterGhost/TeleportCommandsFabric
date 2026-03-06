package org.AndrewElizabeth.teleportcommandsfabric.commands.admin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

final class AdminConfigNodeFactory {

	private AdminConfigNodeFactory() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> intNode(
			String literalName,
			String argName,
			int min,
			IntConsumer setter,
			String messageKey) {
		return Commands.literal(literalName)
				.then(Commands.argument(argName, IntegerArgumentType.integer(min))
						.executes(context -> AdminMessages.setAndSave(
								context,
								() -> setter.accept(IntegerArgumentType.getInteger(context, argName)),
								AdminMessages.t(context.getSource(), messageKey, AdminMessages.intArg(context, argName)))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> boolNode(
			String literalName,
			Consumer<Boolean> setter,
			String messageKey) {
		return Commands.literal(literalName)
				.then(Commands.literal("true")
						.executes(context -> AdminMessages.setAndSave(
								context,
								() -> setter.accept(true),
								AdminMessages.t(context.getSource(), messageKey,
										AdminMessages.t(context.getSource(),
												"commands.teleport_commands.admin.state.enabled")))))
				.then(Commands.literal("false")
						.executes(context -> AdminMessages.setAndSave(
								context,
								() -> setter.accept(false),
								AdminMessages.t(context.getSource(), messageKey,
										AdminMessages.t(context.getSource(),
												"commands.teleport_commands.admin.state.disabled")))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> stringNode(
			String literalName,
			String argName,
			Consumer<String> setter,
			String messageKey) {
		return Commands.literal(literalName)
				.then(Commands.argument(argName, StringArgumentType.string())
						.executes(context -> {
							String value = StringArgumentType.getString(context, argName);
							return AdminMessages.setAndSave(
									context,
									() -> setter.accept(value),
									AdminMessages.t(context.getSource(), messageKey, Component.literal(value)));
						}));
	}
}
