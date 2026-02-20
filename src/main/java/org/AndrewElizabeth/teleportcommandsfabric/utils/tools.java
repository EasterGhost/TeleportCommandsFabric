package org.AndrewElizabeth.teleportcommandsfabric.utils;

import com.google.gson.*;
import org.AndrewElizabeth.teleportcommandsfabric.Constants;
import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.storage.ConfigManager;
import org.AndrewElizabeth.teleportcommandsfabric.storage.TeleportCooldownManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.NotNull;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import static org.AndrewElizabeth.teleportcommandsfabric.Constants.MOD_ID;
import static net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT;

public class tools {

    private static final Set<String> unsafeCollisionFreeBlocks = Set.of("block.minecraft.lava", "block.minecraft.flowing_lava", "block.minecraft.end_portal", "block.minecraft.end_gateway","block.minecraft.fire", "block.minecraft.soul_fire", "block.minecraft.powder_snow", "block.minecraft.nether_portal");

    /**
     * Teleport with delay and cooldown checks
     * @param player Player to teleport
     * @param world Target world
     * @param coords Target coordinates
     * @param bypassDelay If true, skip delay but still check cooldown
     * @return true if teleport was initiated, false if on cooldown
     */
    public static boolean TeleporterWithDelayAndCooldown(ServerPlayer player, ServerLevel world, Vec3 coords, boolean bypassDelay) {
        String uuid = player.getStringUUID();
        int delay = ConfigManager.CONFIG.getTeleporting().getDelay();
        int cooldown = ConfigManager.CONFIG.getTeleporting().getCooldown();
        
        // Check cooldown
        int remainingCooldown = TeleportCooldownManager.getRemainingCooldown(uuid, cooldown);
        if (remainingCooldown > 0) {
            player.displayClientMessage(
                getTranslatedText("commands.teleport_commands.common.cooldown", player, 
                    Component.literal(String.valueOf(remainingCooldown)))
                    .withStyle(ChatFormatting.RED),
                true
            );
            return false;
        }
        
        // If no delay or bypassed, teleport immediately
        if (delay == 0 || bypassDelay) {
            Teleporter(player, world, coords);
            TeleportCooldownManager.updateLastTeleportTime(uuid);
            return true;
        }
        
        // Schedule delayed teleport
        long teleportId = TeleportCooldownManager.scheduleTeleport(uuid);
        
        player.displayClientMessage(
            getTranslatedText("commands.teleport_commands.common.delayStart", player,
                Component.literal(String.valueOf(delay)))
                .withStyle(ChatFormatting.YELLOW),
            true
        );
        
        // Use server scheduler for delay
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // Check if teleport is still valid (not cancelled)
                if (!TeleportCooldownManager.isScheduledTeleportValid(uuid, teleportId)) {
                    return; // Cancelled
                }
                
                // Check if player is still online
                if (player.hasDisconnected()) {
                    TeleportCooldownManager.cancelScheduledTeleport(uuid);
                    return;
                }
                
                // Execute teleport
                Teleporter(player, world, coords);
                TeleportCooldownManager.updateLastTeleportTime(uuid);
                TeleportCooldownManager.cancelScheduledTeleport(uuid);
            }
        }, delay * 1000L);
        
        return true;
    }

    /**
     * Immediate teleport without any checks (for internal use or admin commands)
     */
    public static void Teleporter(ServerPlayer player, ServerLevel world, Vec3 coords) {
        // teleportation effects & sounds before teleporting
        world.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1, player.getZ(), 20, 0.0D, 0.0D, 0.0D, 0.01);
        world.sendParticles(ParticleTypes.WHITE_SMOKE, player.getX(), player.getY(), player.getZ(), 15, 0.0D, 1.0D, 0.0D, 0.03);
        world.playSound(null, player.blockPosition(), SoundEvent.createVariableRangeEvent(ENDERMAN_TELEPORT.location()), SoundSource.PLAYERS, 0.4f, 1.0f);

        // check if the player is currently flying
        boolean flying = player.getAbilities().flying;

        // teleport!
        player.teleportTo(world, coords.x, coords.y, coords.z, Set.of(), player.getYRot(), player.getXRot(), false);

        // Restore flying when teleporting trough dimensions
        if (flying) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }

        // teleportation sound after teleport
        world.playSound(null, player.blockPosition(), SoundEvent.createVariableRangeEvent(ENDERMAN_TELEPORT.location()), SoundSource.PLAYERS, 0.4f, 1.0f);

        // delay visual effects so the player can see it when switching dimensions
        Timer timer = new Timer();
        timer.schedule(
            new TimerTask() {
                @Override
                public void run() {
                    world.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() , player.getZ(), 20, 0.0D, 1.0D, 0.0D, 0.01);
                    world.sendParticles(ParticleTypes.WHITE_SMOKE, player.getX(), player.getY(), player.getZ(), 15, 0.0D, 0.0D, 0.0D, 0.03);
                }
            }, 100 // hopefully a good delay, ~ 2 ticks
        );
    }


    // checks a 7x7x7 location around the player in order to find a safe place to teleport them to.
    public static Optional<BlockPos> getSafeBlockPos(BlockPos blockPos, ServerLevel world) {
        int row = 1;
        int rows = 3;

        int blockPosX = blockPos.getX();
        int blockPosY = blockPos.getY();
        int blockPosZ = blockPos.getZ();

        if (isBlockPosSafe(blockPos, world)) {
            return Optional.of(blockPos); // safe location found!

        } else {
            // find a safe location in an x row radius
            while (row <= rows) {
    //            TeleportCommands.LOGGER.info("currently doing row " + row + " of " + rows); //debug
                for (int z = -row; z <= row; z++) {
                    for (int x = -row; x <= row; x++) {
                        for (int y = -row; y <= row; y++) {

                            // checks if we are on the outer layer of the row, not on the inside
                            if ((x == -row || x == row) || (z == -row || z == row) || (y == -row || y == row)) {

                                // calculate a new blockPos based on the offset we generated
                                BlockPos newPos = new BlockPos(blockPosX + x, blockPosY + y, blockPosZ + z);

                                if (isBlockPosSafe(newPos, world)) {
                                    return Optional.of(newPos);
                                }
                            }
                        }
                    }
                }

                row++;
            }

            // no safe location
            return Optional.empty(); // no safe location found!
        }
    }


    // Gets the translated text for each player based on their language, this is fully server side and actually works (UNLIKE MOJANG'S TRANSLATED KEY'S WHICH ARE CLIENT SIDE) (I'm not mad, I swear!)
    public static @NotNull MutableComponent getTranslatedText(String key, ServerPlayer player, MutableComponent... args) {
        String language = player.clientInformation().language().toLowerCase();
        String regex = "%(\\d+)%";
        Pattern pattern = Pattern.compile(regex);

        // the try catch stuff is so wacky, but it works fine and I don't need to check everything
        try {
            String filePath = String.format("/assets/%s/lang/%s.json", MOD_ID, language);
            InputStream stream = TeleportCommands.class.getResourceAsStream(filePath);

            Reader reader = new InputStreamReader(Objects.requireNonNull(stream, String.format("Couldn't find the required language file for \"%s\"", language)), StandardCharsets.UTF_8);
            JsonElement json = JsonParser.parseReader(reader);
            String translation = json.getAsJsonObject().get(key).getAsString();


            // Adds the optional MutableComponents in the correct places
            Matcher matcher = pattern.matcher(Objects.requireNonNull(translation));

            MutableComponent component = Component.literal("");
            int lastIndex = 0;

            while (matcher.find()) {
                component.append(Component.literal(translation.substring(lastIndex, matcher.start())));

                int index = Integer.parseInt(matcher.group(1));
                component.append(args[index]);

                lastIndex = matcher.end();
            }
            component.append(translation.substring(lastIndex));

            return component;

        } catch (Exception e) {

            try {
                if (!Objects.equals(language, "en_us")) {
                    String filePath = String.format("/assets/%s/lang/en_us.json", MOD_ID);
                    InputStream stream = TeleportCommands.class.getResourceAsStream(filePath);

                    Reader reader = new InputStreamReader(Objects.requireNonNull(stream, String.format("Couldn't find the required language file for \"%s\"", language)), StandardCharsets.UTF_8);
                    JsonElement json = JsonParser.parseReader(reader);
                    String translation = json.getAsJsonObject().get(key).getAsString();

                    // Adds the optional MutableComponents in the correct places
                    Matcher matcher = pattern.matcher(Objects.requireNonNull(translation, "translation cannot be null"));

                    MutableComponent component = Component.literal("");
                    int lastIndex = 0;

                    while (matcher.find()) {
                        component.append(Component.literal(translation.substring(lastIndex, matcher.start())));

                        int index = Integer.parseInt(matcher.group(1));
                        component.append(args[index]);

                        lastIndex = matcher.end();
                    }
                    component.append(translation.substring(lastIndex));

                    return component;
                }
            } catch (Exception ignored1) {}
            Constants.LOGGER.error("Key \"{}\" not found in the default language (en_us), sending raw key as fallback.", key);
            return Component.literal(key);
        }
    }


    public static String getDimensionId(ResourceKey<Level> dimensionKey) {
        // ResourceKey#location() missing in this mapping; parse id from toString form
        String raw = dimensionKey.toString();
        int splitIndex = raw.indexOf("/ ");
        if (splitIndex >= 0 && raw.endsWith("]")) {
            return raw.substring(splitIndex + 2, raw.length() - 1);
        }
        return raw;
    }


    // Gets the ids of all the worlds
    public static List<String> getWorldIds() {
        return StreamSupport.stream(TeleportCommands.SERVER.getAllLevels().spliterator(), false)
                .map(level -> getDimensionId(level.dimension()))
                .toList();
    }


    // checks if a BlockPos is safe, used by the teleportSafetyChecker.
    private static boolean isBlockPosSafe(BlockPos bottomPlayer, ServerLevel world) {

        // get the block below the player
        BlockPos belowPlayer = new BlockPos(bottomPlayer.getX(), bottomPlayer.getY() -1, bottomPlayer.getZ()); // below the player
        String belowPlayerId = world.getBlockState(belowPlayer).getBlock().getDescriptionId(); // below the player

        // get the bottom of the player
        String BottomPlayerId = world.getBlockState(bottomPlayer).getBlock().getDescriptionId(); // bottom of player

        // get the top of the player
        BlockPos TopPlayer = new BlockPos(bottomPlayer.getX(), bottomPlayer.getY() + 1, bottomPlayer.getZ()); // top of player
        String TopPlayerId = world.getBlockState(TopPlayer).getBlock().getDescriptionId(); // top of player


        // check if the block position isn't safe
        if ((belowPlayerId.equals("block.minecraft.water") || !world.getBlockState(belowPlayer).getCollisionShape(world, belowPlayer).isEmpty()) // check if the player is going to fall on teleport
            && (world.getBlockState(bottomPlayer).getCollisionShape(world, bottomPlayer).isEmpty() && !unsafeCollisionFreeBlocks.contains(BottomPlayerId)) // check if it is a collision free block that isn't dangerous
            && (!unsafeCollisionFreeBlocks.contains(TopPlayerId))) // check if it is a dangerous collision free block, if it is solid then the player crawls
        {
            return true; // it's safe
        }
        return false; // it's not safe!
    }
}
