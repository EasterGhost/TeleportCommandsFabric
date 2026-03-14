package org.AndrewElizabeth.teleportcommandsfabric.commands.admin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

final class AdminConfigNodeFactory {

	private AdminConfigNodeFactory() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> intNode(
			String literalName,
			String argName,
			int min,
			IntSupplier getter,
			IntConsumer setter,
			String messageKey) {
		return Commands.literal(literalName)
				.executes(context -> AdminMessages.sendCurrentValue(
						context.getSource(),
						literalName,
						Component.literal(String.valueOf(getter.getAsInt()))))
				.then(Commands.argument(argName, IntegerArgumentType.integer(min))
						.executes(context -> AdminMessages.setAndSave(
								context,
								() -> setter.accept(IntegerArgumentType.getInteger(context, argName)),
								AdminMessages.t(context.getSource(), messageKey, AdminMessages.intArg(context, argName)))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> intNode(
			String literalName,
			String argName,
			int min,
			int max,
			IntSupplier getter,
			IntConsumer setter,
			String messageKey) {
		return Commands.literal(literalName)
				.executes(context -> AdminMessages.sendCurrentValue(
						context.getSource(),
						literalName,
						Component.literal(String.valueOf(getter.getAsInt()))))
				.then(Commands.argument(argName, IntegerArgumentType.integer(min, max))
						.executes(context -> AdminMessages.setAndSave(
								context,
								() -> setter.accept(IntegerArgumentType.getInteger(context, argName)),
								AdminMessages.t(context.getSource(), messageKey, AdminMessages.intArg(context, argName)))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> boolNode(
			String literalName,
			Supplier<Boolean> getter,
			Consumer<Boolean> setter,
			String messageKey) {
		return Commands.literal(literalName)
				.executes(context -> AdminMessages.sendCurrentValue(
						context.getSource(),
						literalName,
						AdminMessages.t(context.getSource(),
								getter.get()
										? "commands.teleport_commands.admin.stat.enabled"
										: "commands.teleport_commands.admin.stat.disabled")))
				.then(Commands.literal("true")
						.executes(context -> AdminMessages.setAndSave(
								context,
								() -> setter.accept(true),
								AdminMessages.t(context.getSource(), messageKey,
										AdminMessages.t(context.getSource(),
												"commands.teleport_commands.admin.stat.enabled")))))
				.then(Commands.literal("false")
						.executes(context -> AdminMessages.setAndSave(
								context,
								() -> setter.accept(false),
								AdminMessages.t(context.getSource(), messageKey,
										AdminMessages.t(context.getSource(),
												"commands.teleport_commands.admin.stat.disabled")))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> stringNode(
			String literalName,
			String argName,
			Supplier<String> getter,
			Consumer<String> setter,
			String messageKey) {
		return Commands.literal(literalName)
				.executes(context -> AdminMessages.sendCurrentValue(
						context.getSource(),
						literalName,
						Component.literal(getter.get())))
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
