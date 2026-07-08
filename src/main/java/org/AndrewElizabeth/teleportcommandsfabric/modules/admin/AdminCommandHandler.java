package org.AndrewElizabeth.teleportcommandsfabric.modules.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.config.Config;
import org.AndrewElizabeth.teleportcommandsfabric.config.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.modules.common.CommandReturns;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminHelpRenderer;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminHelpRequest;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminStatusRenderer;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

final class AdminCommandHandler {
	private static final AdminHelpRenderer HELP_RENDERER = new AdminHelpRenderer();
	private static final AdminStatusRenderer STATUS_RENDERER = new AdminStatusRenderer();

	private AdminCommandHandler() {
	}

	static int reload(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		ConfigManager.reload().whenComplete((ignored, throwable) -> source.getServer().execute(() -> {
			if (throwable != null) {
				ModConstants.LOGGER.error("Failed to reload config.", throwable);
				AdminMessages.sendError(source, AdminMessages.t(source,
						"commands.teleport_commands.admin.reload.error",
						Component.literal(String.valueOf(throwable.getMessage()))));
			} else {
				AdminMessages.sendSuccess(source,
						AdminMessages.t(source, "commands.teleport_commands.admin.reload.success"), true);
			}
		}));
		return CommandReturns.ACCEPTED_ASYNC;
	}

	static int sendCurrentDebug(CommandContext<CommandSourceStack> context) {
		return AdminMessages.sendCurrentValue(context.getSource(), "debug",
				AdminMessages.enabledText(context.getSource(), ConfigManager.query(Config::isDebugEnabled)));
	}

	static int setDebug(CommandContext<CommandSourceStack> context, boolean enabled)
			throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		try {
			ConfigManager.mutate(config -> config.setDebugEnabled(enabled));
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Failed to update debug logging.", exception);
			throw saveException(source, exception);
		}

		AdminMessages.sendSuccess(source, AdminMessages.t(source,
				"commands.teleport_commands.admin.debug",
				AdminMessages.enabledText(source, enabled)), true);
		return CommandReturns.COMPLETED_SYNC;
	}

	static int sendStatus(CommandSourceStack source) {
		source.sendSuccess(() -> STATUS_RENDERER.render(AdminModuleRegistry.statuses(), AdminMessages.language(source),
				AdminRuntimeInfoProvider.current()), false);
		return CommandReturns.COMPLETED_SYNC;
	}

	static int sendHelp(CommandSourceStack source, AdminHelpRequest request) {
		source.sendSuccess(() -> HELP_RENDERER.render(request), false);
		return CommandReturns.COMPLETED_SYNC;
	}

	static void sendStatusRefreshDivider(CommandSourceStack source) {
		source.sendSuccess(STATUS_RENDERER::renderRefreshDivider, false);
	}

	static int toggleModule(CommandContext<CommandSourceStack> context, boolean enabled)
			throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		String moduleName = StringArgumentType.getString(context, "module");
		AdminModuleRegistry.ModuleToggle toggle = AdminModuleRegistry.get(moduleName);

		if (toggle == null) {
			throw new SimpleCommandExceptionType(AdminMessages.t(source,
					"commands.teleport_commands.admin.module.unavailable", Component.literal(moduleName))
					.withStyle(ChatFormatting.RED, ChatFormatting.BOLD)).create();
		}

		try {
			AdminModuleRegistry.setEnabled(moduleName, enabled);
		} catch (Exception exception) {
			ModConstants.LOGGER.error("Failed to toggle admin module: {}", moduleName, exception);
			throw saveException(source, exception);
		}

		AdminMessages.sendSuccess(source, AdminMessages.t(source,
				"commands.teleport_commands.admin.module.status",
				AdminMessages.t(source, toggle.labelKey()),
				AdminMessages.enabledText(source, enabled)), true);
		sendStatusRefreshDivider(source);
		return sendStatus(source);
	}

	private static CommandSyntaxException saveException(CommandSourceStack source, Exception exception) {
		return new SimpleCommandExceptionType(AdminMessages.t(source,
				"commands.teleport_commands.admin.save.error",
				Component.literal(String.valueOf(exception.getMessage()))).withStyle(ChatFormatting.RED)).create();
	}
}
