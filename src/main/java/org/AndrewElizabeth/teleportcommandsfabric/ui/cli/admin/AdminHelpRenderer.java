package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin;

import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.ComponentSupport;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Objects;

public final class AdminHelpRenderer {
	private static final String ROOT_COMMAND = "/tpc";
	private static final List<HelpEntry> ADMIN_ENTRIES = List.of(
			new HelpEntry("help", "/tpc help", "/tpc help", "commands.teleport_commands.admin.help.desc.help"),
			new HelpEntry("status", "/tpc status", "/tpc status", "commands.teleport_commands.admin.help.desc.status"),
			new HelpEntry("reload", "/tpc reload", "/tpc reload", "commands.teleport_commands.admin.help.desc.reload"),
			new HelpEntry("debug", "/tpc debug <true|false>", "/tpc debug ",
					"commands.teleport_commands.admin.help.desc.debug"),
			new HelpEntry("enable", "/tpc enable <module>", "/tpc enable ", "commands.teleport_commands.admin.help.desc.enable"),
			new HelpEntry("disable", "/tpc disable <module>", "/tpc disable ", "commands.teleport_commands.admin.help.desc.disable"));
	private static final List<ConfigGroup> CONFIG_GROUPS = List.of(
			new ConfigGroup("teleporting", List.of(
					config("delay", "/tpc config teleporting delay <seconds>", "/tpc config teleporting delay ",
							"commands.teleport_commands.admin.help.desc.config.teleporting.delay"),
					config("cooldown", "/tpc config teleporting cooldown <seconds>", "/tpc config teleporting cooldown ",
							"commands.teleport_commands.admin.help.desc.config.teleporting.cooldown"),
					config("effects", "/tpc config teleporting effects <true|false>", "/tpc config teleporting effects ",
							"commands.teleport_commands.admin.help.desc.config.teleporting.effects"),
					config("restoreRotation", "/tpc config teleporting restoreRotation <true|false>",
							"/tpc config teleporting restoreRotation ",
							"commands.teleport_commands.admin.help.desc.config.teleporting.restoreRotation"),
					config("preload", "/tpc config teleporting preload <true|false>", "/tpc config teleporting preload ",
							"commands.teleport_commands.admin.help.desc.config.teleporting.preload"),
					config("preloadRadius", "/tpc config teleporting preloadRadius <chunks>",
							"/tpc config teleporting preloadRadius ",
							"commands.teleport_commands.admin.help.desc.config.teleporting.preloadRadius"),
					config("defaultSafetyCheck", "/tpc config teleporting defaultSafetyCheck <true|false>",
							"/tpc config teleporting defaultSafetyCheck ",
							"commands.teleport_commands.admin.help.desc.config.teleporting.defaultSafetyCheck"))),
			new ConfigGroup("back", List.of(
					config("deleteAfterTeleport", "/tpc config back deleteAfterTeleport <true|false>",
							"/tpc config back deleteAfterTeleport ",
							"commands.teleport_commands.admin.help.desc.config.back.deleteAfterTeleport"))),
			new ConfigGroup("home", List.of(
					config("max", "/tpc config home max <count>", "/tpc config home max ",
							"commands.teleport_commands.admin.help.desc.config.home.max"),
					config("deleteInvalid", "/tpc config home deleteInvalid <true|false>", "/tpc config home deleteInvalid ",
							"commands.teleport_commands.admin.help.desc.config.home.deleteInvalid"),
					config("temporaryHomeTtl", "/tpc config home temporaryHomeTtl <seconds>",
							"/tpc config home temporaryHomeTtl ",
							"commands.teleport_commands.admin.help.desc.config.home.temporaryHomeTtl"),
					config("sharedMax", "/tpc config home sharedMax <count>",
							"/tpc config home sharedMax ",
							"commands.teleport_commands.admin.help.desc.config.home.sharedMax"),
					config("sharedBroadcastCooldown", "/tpc config home sharedBroadcastCooldown <seconds>",
							"/tpc config home sharedBroadcastCooldown ",
							"commands.teleport_commands.admin.help.desc.config.home.sharedBroadcastCooldown"))),
			new ConfigGroup("tpa", List.of(
					config("expireTime", "/tpc config tpa expireTime <seconds>", "/tpc config tpa expireTime ",
							"commands.teleport_commands.admin.help.desc.config.tpa.expireTime"))),
			new ConfigGroup("warp", List.of(
					config("max", "/tpc config warp max <count>", "/tpc config warp max ",
							"commands.teleport_commands.admin.help.desc.config.warp.max"),
					config("deleteInvalid", "/tpc config warp deleteInvalid <true|false>", "/tpc config warp deleteInvalid ",
							"commands.teleport_commands.admin.help.desc.config.warp.deleteInvalid"))),
			new ConfigGroup("worldspawn", List.of(
					config("world", "/tpc config worldspawn world <worldId>", "/tpc config worldspawn world ",
							"commands.teleport_commands.admin.help.desc.config.worldspawn.world"))),
			new ConfigGroup("rtp", List.of(
					config("minRadius", "/tpc config rtp minRadius <blocks>", "/tpc config rtp minRadius ",
							"commands.teleport_commands.admin.help.desc.config.rtp.minRadius"),
					config("maxRadius", "/tpc config rtp maxRadius <blocks>", "/tpc config rtp maxRadius ",
							"commands.teleport_commands.admin.help.desc.config.rtp.maxRadius"))),
			new ConfigGroup("wild", List.of(
					config("minRadius", "/tpc config wild minRadius <blocks>", "/tpc config wild minRadius ",
							"commands.teleport_commands.admin.help.desc.config.wild.minRadius"),
					config("maxRadius", "/tpc config wild maxRadius <blocks>", "/tpc config wild maxRadius ",
							"commands.teleport_commands.admin.help.desc.config.wild.maxRadius"))),
			new ConfigGroup("integration", List.of(
					config("syncIntervalSeconds", "/tpc config integration syncIntervalSeconds <seconds>",
							"/tpc config integration syncIntervalSeconds ",
							"commands.teleport_commands.admin.help.desc.config.integration.syncIntervalSeconds"))),
			new ConfigGroup("storage", List.of(
					config("autoSaveIntervalSeconds", "/tpc config storage autoSaveIntervalSeconds <seconds>",
							"/tpc config storage autoSaveIntervalSeconds ",
							"commands.teleport_commands.admin.help.desc.config.storage.autoSaveIntervalSeconds"))));

	public Component render(AdminHelpRequest request) {
		AdminHelpRequest safeRequest = Objects.requireNonNull(request, "request");
		return switch (safeRequest.topic()) {
		case OVERVIEW -> renderOverview(safeRequest);
		case ADMIN -> renderAdmin(safeRequest);
		case CONFIG -> renderConfigIndex(safeRequest);
		case CONFIG_MODULE -> renderConfigModule(safeRequest);
		};
	}

	private MutableComponent renderOverview(AdminHelpRequest request) {
		MutableComponent message = Component.empty();
		appendTitle(message, "commands.teleport_commands.admin.help.title", request.language());
		appendVersion(message, request);
		appendSection(message, "commands.teleport_commands.admin.help.section.topics", request.language());
		appendLine(message, Component.empty()
				.append(topicButton("commands.teleport_commands.admin.help.topic.admin", "/tpc help admin", request.language()))
				.append(" ")
				.append(topicButton("commands.teleport_commands.admin.help.topic.config", "/tpc help config", request.language())));
		appendSection(message, "commands.teleport_commands.admin.help.section.quick", request.language());
		MutableComponent quick = Component.empty();
		for (HelpEntry entry : ADMIN_ENTRIES.subList(1, ADMIN_ENTRIES.size())) {
			if (!quick.getString().isEmpty()) {
				quick.append(" ");
			}
			quick.append(suggestButton(entry.label(), entry.suggestion(), entry.usage(), request.language()));
		}
		appendLine(message, quick);
		return message;
	}

	private MutableComponent renderAdmin(AdminHelpRequest request) {
		MutableComponent message = Component.empty();
		appendTitle(message, "commands.teleport_commands.admin.help.title.admin", request.language());
		for (HelpEntry entry : ADMIN_ENTRIES) {
			appendEntry(message, entry, request.language());
		}
		appendSection(message, "commands.teleport_commands.admin.help.section.modules", request.language());
		appendLine(message, Component.literal("back home tpa warp worldspawn rtp wild integration").withStyle(ChatFormatting.GRAY));
		return message;
	}

	private MutableComponent renderConfigIndex(AdminHelpRequest request) {
		MutableComponent message = Component.empty();
		appendTitle(message, "commands.teleport_commands.admin.help.title.config", request.language());
		appendSection(message, "commands.teleport_commands.admin.help.section.modules", request.language());
		for (int index = 0; index < CONFIG_GROUPS.size(); index += 4) {
			MutableComponent row = Component.empty();
			for (int offset = 0; offset < 4 && index + offset < CONFIG_GROUPS.size(); offset++) {
				if (offset > 0) {
					row.append(" ");
				}
				ConfigGroup group = CONFIG_GROUPS.get(index + offset);
				row.append(topicButtonLiteral(group.name(), ROOT_COMMAND + " help config " + group.name(), request.language()));
			}
			appendLine(message, row);
		}
		return message;
	}

	private MutableComponent renderConfigModule(AdminHelpRequest request) {
		ConfigGroup group = configGroup(request.module());
		if (group == null) {
			return renderConfigIndex(request);
		}

		MutableComponent message = Component.empty();
		appendTitle(message, "commands.teleport_commands.admin.help.title.configModule", request.language(),
				Component.literal(group.name()));
		for (HelpEntry entry : group.entries()) {
			appendEntry(message, entry, request.language());
		}
		return message;
	}

	private void appendTitle(MutableComponent message, String titleKey, String language, MutableComponent... args) {
		message.append(Component.literal("========== ").withStyle(ChatFormatting.DARK_GRAY));
		message.append(ComponentSupport.translate(titleKey, language, args).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
		message.append(Component.literal(" ==========").withStyle(ChatFormatting.DARK_GRAY));
	}

	private void appendVersion(MutableComponent message, AdminHelpRequest request) {
		AdminInfoRenderer.append(message, request.runtimeInfo(), request.language());
	}

	private void appendSection(MutableComponent message, String key, String language) {
		appendLine(message, ComponentSupport.translate(key, language).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	private void appendEntry(MutableComponent message, HelpEntry entry, String language) {
		appendLine(message, commandUsage(entry.usage(), entry.suggestion(), language));
		appendLine(message, Component.literal("  ")
				.append(ComponentSupport.translate(entry.descriptionKey(), language).withStyle(ChatFormatting.GRAY)));
	}

	private MutableComponent commandUsage(String usage, String suggestion, String language) {
		return Component.literal(usage)
				.withStyle(ChatFormatting.YELLOW)
				.withStyle(style -> style
						.withClickEvent(new ClickEvent.SuggestCommand(suggestion))
						.withHoverEvent(suggestHover(language, suggestion)));
	}

	private MutableComponent topicButton(String labelKey, String command, String language) {
		return Component.literal("[")
				.append(ComponentSupport.translate(labelKey, language))
				.append("]")
				.withStyle(ChatFormatting.AQUA)
				.withStyle(style -> style
						.withClickEvent(new ClickEvent.RunCommand(command))
						.withHoverEvent(openHover(language, command)));
	}

	private MutableComponent topicButtonLiteral(String label, String command, String language) {
		return Component.literal("[" + label + "]")
				.withStyle(ChatFormatting.AQUA)
				.withStyle(style -> style
						.withClickEvent(new ClickEvent.RunCommand(command))
						.withHoverEvent(openHover(language, command)));
	}

	private MutableComponent suggestButton(String label, String suggestion, String usage, String language) {
		return Component.literal("[" + label + "]")
				.withStyle(ChatFormatting.YELLOW)
				.withStyle(style -> style
						.withClickEvent(new ClickEvent.SuggestCommand(suggestion))
						.withHoverEvent(suggestHover(language, usage)));
	}

	private HoverEvent openHover(String language, String command) {
		return new HoverEvent.ShowText(ComponentSupport.translate(
				"commands.teleport_commands.admin.help.hoverOpen", language, Component.literal(command)));
	}

	private HoverEvent suggestHover(String language, String command) {
		return new HoverEvent.ShowText(ComponentSupport.translate(
				"commands.teleport_commands.admin.help.hoverSuggest", language, Component.literal(command)));
	}

	private void appendLine(MutableComponent message, MutableComponent line) {
		message.append("\n");
		message.append(line);
	}

	private ConfigGroup configGroup(String module) {
		return CONFIG_GROUPS.stream()
				.filter(group -> group.name().equals(module))
				.findFirst()
				.orElse(null);
	}

	private static HelpEntry config(String label, String usage, String suggestion, String descriptionKey) {
		return new HelpEntry(label, usage, suggestion, descriptionKey);
	}

	private record HelpEntry(String label, String usage, String suggestion, String descriptionKey) {
	}

	private record ConfigGroup(String name, List<HelpEntry> entries) {
	}
}
