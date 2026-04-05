package org.AndrewElizabeth.teleportcommandsfabric.core.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class PagedNodeCommandSupport {
	private static final String PAGE_ARGUMENT = "page";
	private static final String DIMENSION_ARGUMENT = "dimension";

	@FunctionalInterface
	public interface PageDimensionExecutor {
		int run(CommandContext<CommandSourceStack> context, int page, String dimension) throws CommandSyntaxException;
	}

	private PagedNodeCommandSupport() {
	}

	public static RequiredArgumentBuilder<CommandSourceStack, Integer> pageWithOptionalDimension(
			SuggestionProvider<CommandSourceStack> dimensionSuggestionProvider, PageDimensionExecutor executor) {
		RequiredArgumentBuilder<CommandSourceStack, Integer> pageArgument = Commands.argument(PAGE_ARGUMENT,
				IntegerArgumentType.integer())
				.executes(context -> executor.run(context, IntegerArgumentType.getInteger(context, PAGE_ARGUMENT), null));

		pageArgument.then(createDimensionArgument(dimensionSuggestionProvider)
				.executes(context -> executor.run(context, IntegerArgumentType.getInteger(context, PAGE_ARGUMENT),
						StringArgumentType.getString(context, DIMENSION_ARGUMENT))));
		return pageArgument;
	}

	public static RequiredArgumentBuilder<CommandSourceStack, String> dimensionOnly(int defaultPage,
			SuggestionProvider<CommandSourceStack> dimensionSuggestionProvider, PageDimensionExecutor executor) {
		return createDimensionArgument(dimensionSuggestionProvider)
				.executes(context -> executor.run(context, defaultPage,
						StringArgumentType.getString(context, DIMENSION_ARGUMENT)));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> createDimensionArgument(
			SuggestionProvider<CommandSourceStack> dimensionSuggestionProvider) {
		RequiredArgumentBuilder<CommandSourceStack, String> dimensionArgument = Commands.argument(DIMENSION_ARGUMENT,
				StringArgumentType.string());
		if (dimensionSuggestionProvider != null) {
			dimensionArgument = dimensionArgument.suggests(dimensionSuggestionProvider);
		}
		return dimensionArgument;
	}
}
