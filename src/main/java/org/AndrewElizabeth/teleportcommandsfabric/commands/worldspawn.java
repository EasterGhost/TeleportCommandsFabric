package org.AndrewElizabeth.teleportcommandsfabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.AndrewElizabeth.teleportcommandsfabric.utils.tools.*;
import static net.minecraft.commands.Commands.argument;

import static net.minecraft.world.level.Level.OVERWORLD;

public class worldspawn {

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(Commands.literal("worldspawn")
                .requires(source -> source.getPlayer() != null)
                .executes(context -> {
                    final ServerPlayer player = context.getSource().getPlayerOrException();

                    if (!ConfigManager.CONFIG.getWorldSpawn().isEnabled()) {
                        player.displayClientMessage(getTranslatedText("commands.teleport_commands.worldspawn.disabled", player)
                                .withStyle(ChatFormatting.RED), true);
                        return 1;
                    }

                    try {
                        toWorldSpawn(player, false);

                    } catch (Exception error) {
                        Constants.LOGGER.error("Error while going to the worldspawn! => ", error);
                        player.displayClientMessage(getTranslatedText("commands.teleport_commands.common.error", player).withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
                        return 1;
                    }
                    return 0;
                })
                .then(argument("Disable Safety", BoolArgumentType.bool())
                        .requires(source -> source.getPlayer() != null)
                        .executes(context -> {
                            final boolean safety = BoolArgumentType.getBool(context, "Disable Safety");
                            final ServerPlayer player = context.getSource().getPlayerOrException();

                            if (!ConfigManager.CONFIG.getWorldSpawn().isEnabled()) {
                                player.displayClientMessage(getTranslatedText("commands.teleport_commands.worldspawn.disabled", player)
                                        .withStyle(ChatFormatting.RED), true);
                                return 1;
                            }

                            try {
                                toWorldSpawn(player, safety);

                            } catch (Exception error) {
                                Constants.LOGGER.error("Error while going to the worldspawn! => ", error);
                                player.displayClientMessage(getTranslatedText("commands.teleport_commands.common.error", player).withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
                                return 1;
                            }
                            return 0;
                        })));


    }

    private static void toWorldSpawn(ServerPlayer player, boolean safetyDisabled) throws NullPointerException {
        // Get world from config
        String worldId = ConfigManager.CONFIG.getWorldSpawn().getWorld_id();
        ServerLevel world = StreamSupport.stream(TeleportCommands.SERVER.getAllLevels().spliterator(), false)
                .filter(level -> Objects.equals(getDimensionId(level.dimension()), worldId))
                .findFirst()
                .orElse(null);

        if (world == null) {
            Constants.LOGGER.error("World not found: {}, falling back to overworld", worldId);
            world = TeleportCommands.SERVER.getLevel(OVERWORLD);
        }
        
        BlockPos worldSpawn = Objects.requireNonNull(world, "World cannot be null!").getLevelData().getRespawnData().pos();

        if (!safetyDisabled) {
            Optional<BlockPos> teleportData = getSafeBlockPos(worldSpawn, world);

            if (teleportData.isPresent()) {
                BlockPos safeBlockPos = teleportData.get();

                // check if the player is already at this location
                if (player.blockPosition().equals(safeBlockPos) && player.level() == world) {

                    player.displayClientMessage(getTranslatedText("commands.teleport_commands.worldspawn.same", player).withStyle(ChatFormatting.AQUA), true);
                } else {
                    Vec3 teleportPos = new Vec3(safeBlockPos.getX() + 0.5, safeBlockPos.getY(), safeBlockPos.getZ() + 0.5);

                    player.displayClientMessage(getTranslatedText("commands.teleport_commands.worldspawn.go", player), true);
                    TeleporterWithDelayAndCooldown(player, world, teleportPos, false);
                }

            } else {

                player.displayClientMessage(
                        Component.empty()
                        .append(getTranslatedText("commands.teleport_commands.common.noSafeLocation", player)
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                        )
                        .append("\n")
                        .append(getTranslatedText("commands.teleport_commands.common.safetyIsForLosers", player)
                                .withStyle(ChatFormatting.WHITE)
                        )
                        .append("\n")
                        .append(getTranslatedText("commands.teleport_commands.common.forceTeleport", player)
                                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)
                                .withStyle(style -> style.withClickEvent(
                                                new ClickEvent.RunCommand("/worldspawn true")
                                        )
                                )
                        )
                        .append("\n"), false);
            }

        } else {

            if (player.blockPosition().equals(worldSpawn) && player.level() == world) {

                player.displayClientMessage(getTranslatedText("commands.teleport_commands.worldspawn.same", player).withStyle(ChatFormatting.AQUA), true);
            } else {

                player.displayClientMessage(getTranslatedText("commands.teleport_commands.worldspawn.go", player), true);
                TeleporterWithDelayAndCooldown(player, world, new Vec3(worldSpawn.getX() + 0.5, worldSpawn.getY(), worldSpawn.getZ() + 0.5), false);
            }
        }
    }

}
