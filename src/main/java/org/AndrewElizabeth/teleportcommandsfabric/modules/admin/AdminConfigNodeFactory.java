package org.AndrewElizabeth.teleportcommandsfabric.modules.admin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.config.Config;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Function;

final class AdminConfigNodeFactory {
	private AdminConfigNodeFactory() {
	}

	static LiteralArgumentBuilder<CommandSourceStack> intNode(String literalName, String argName, int min,
			Function<Config, Integer> getter, BiConsumer<Config, Integer> setter, String messageKey) {
		return Commands.literal(literalName)
				.executes(context -> AdminMessages.sendCurrentValue(context.getSource(), literalName,
						Component.literal(String.valueOf(ConfigManager.query(getter)))))
				.then(Commands.argument(argName, IntegerArgumentType.integer(min))
						.executes(context -> mutateAndReply(context,
								config -> setter.accept(config, IntegerArgumentType.getInteger(context, argName)),
								AdminMessages.t(context.getSource(), messageKey, intArg(context, argName)))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> intNode(String literalName, String argName, int min, int max,
			Function<Config, Integer> getter, BiConsumer<Config, Integer> setter, String messageKey) {
		return Commands.literal(literalName)
				.executes(context -> AdminMessages.sendCurrentValue(context.getSource(), literalName,
						Component.literal(String.valueOf(ConfigManager.query(getter)))))
				.then(Commands.argument(argName, IntegerArgumentType.integer(min, max))
						.executes(context -> mutateAndReply(context,
								config -> setter.accept(config, IntegerArgumentType.getInteger(context, argName)),
								AdminMessages.t(context.getSource(), messageKey, intArg(context, argName)))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> durationSecondsNode(String literalName, String argName, int min,
			Function<Config, Duration> getter, BiConsumer<Config, Duration> setter, String messageKey) {
		return Commands.literal(literalName)
				.executes(context -> {
					Long seconds = ConfigManager.query(config -> getter.apply(config).toSeconds());
					return AdminMessages.sendCurrentValue(context.getSource(), literalName,
							Component.literal(Long.toString(seconds)));
				})
				.then(Commands.argument(argName, IntegerArgumentType.integer(min))
						.executes(context -> mutateAndReply(context,
								config -> setter.accept(config,
										Duration.ofSeconds(IntegerArgumentType.getInteger(context, argName))),
								AdminMessages.t(context.getSource(), messageKey, intArg(context, argName)))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> boolNode(String literalName, Function<Config, Boolean> getter,
			BiConsumer<Config, Boolean> setter, String messageKey) {
		return Commands.literal(literalName)
				.executes(context -> AdminMessages.sendCurrentValue(context.getSource(), literalName,
						booleanText(context.getSource(), ConfigManager.query(getter))))
				.then(Commands.literal("true")
						.executes(context -> mutateAndReply(context, config -> setter.accept(config, true),
								AdminMessages.t(context.getSource(), messageKey,
										booleanText(context.getSource(), true)))))
				.then(Commands.literal("false")
						.executes(context -> mutateAndReply(context, config -> setter.accept(config, false),
								AdminMessages.t(context.getSource(), messageKey,
										booleanText(context.getSource(), false)))));
	}

	static LiteralArgumentBuilder<CommandSourceStack> stringNode(String literalName, String argName,
			Function<Config, String> getter, BiConsumer<Config, String> setter, String messageKey) {
		return Commands.literal(literalName)
				.executes(context -> AdminMessages.sendCurrentValue(context.getSource(), literalName,
						Component.literal(ConfigManager.query(getter))))
				.then(Commands.argument(argName, StringArgumentType.string())
						.executes(context -> {
							String value = StringArgumentType.getString(context, argName);
							return mutateAndReply(context, config -> setter.accept(config, value),
									AdminMessages.t(context.getSource(), messageKey, Component.literal(value)));
						}));
	}

	private static int mutateAndReply(CommandContext<CommandSourceStack> context, java.util.function.Consumer<Config> writer,
			MutableComponent message) throws CommandSyntaxException {
		try {
			ConfigManager.mutate(writer);
			AdminMessages.sendSuccess(context.getSource(), message, true);
			return 0;
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Failed to update admin config.", exception);
			throw new SimpleCommandExceptionType(AdminMessages.t(context.getSource(),
					"commands.teleport_commands.admin.save.error",
					Component.literal(String.valueOf(exception.getMessage()))).withStyle(ChatFormatting.RED))
							.create();
		}
	}

	private static MutableComponent intArg(CommandContext<CommandSourceStack> context, String argName) {
		return Component.literal(String.valueOf(IntegerArgumentType.getInteger(context, argName)));
	}

	private static MutableComponent booleanText(CommandSourceStack source, boolean enabled) {
		return AdminMessages.t(source, enabled
				? "commands.teleport_commands.admin.stat.enabled"
				: "commands.teleport_commands.admin.stat.disabled");
	}
}
