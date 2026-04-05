package org.AndrewElizabeth.teleportcommandsfabric.modules.admin;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import org.AndrewElizabeth.teleportcommandsfabric.ModConstants;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.TranslationHelper.getTranslatedText;

final class AdminMessages {

	private AdminMessages() {
	}

	static MutableComponent t(CommandSourceStack source, String key, MutableComponent... args) {
		ServerPlayer player = source.getPlayer();
		return player != null ? getTranslatedText(key, player, args) : getTranslatedText(key, "en_us", args);
	}

	static MutableComponent intArg(CommandContext<CommandSourceStack> context, String argName) {
		return Component.literal(String.valueOf(
				IntegerArgumentType.getInteger(context, argName)));
	}

	static int setAndSave(CommandContext<CommandSourceStack> context, Runnable setter,
			MutableComponent message) throws CommandSyntaxException {
		try {
			setter.run();
			ConfigManager.ConfigSaver();
			context.getSource().sendSuccess(() -> message.copy().withStyle(ChatFormatting.GREEN), true);
			return 0;
		} catch (Exception e) {
			ModConstants.LOGGER.error("Failed to save config!", e);
			throw new SimpleCommandExceptionType(t(context.getSource(),
					"commands.teleport_commands.admin.save.error", Component.literal(e.getMessage())).withStyle(ChatFormatting.RED))
							.create();
		}
	}

	static int sendCurrentValue(CommandSourceStack source, String name, MutableComponent value) {
		source.sendSuccess(
				() -> t(source, "commands.teleport_commands.admin.config.current", Component.literal(name), value)
						.withStyle(ChatFormatting.YELLOW),
				false);
		return 0;
	}
}
