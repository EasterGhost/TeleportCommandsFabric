package org.AndrewElizabeth.teleportcommandsfabric.common;

import org.AndrewElizabeth.teleportcommandsfabric.TeleportCommands;
import org.AndrewElizabeth.teleportcommandsfabric.storage.StorageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class NamedLocation {
    private String name;
    private final int x;
    private final int y;
    private final int z;
    private final String world;

    public NamedLocation(String name, BlockPos pos, String world) {
        this.name = name;
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        this.world = world;
    }

    // -----

    public String getName() {
        return this.name;
    }

    public BlockPos getBlockPos() {
         return new BlockPos(this.x, this.y, this.z);
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    // Return the world id as a string
    public String getWorldString() {
        return this.world;
    }

    // function to quickly filter the worlds and get the ServerLevel for the string
    public Optional<ServerLevel> getWorld() {
        return StreamSupport.stream(TeleportCommands.SERVER.getAllLevels().spliterator(), false) // woa, this looks silly
                .filter(level -> Objects.equals(getDimensionId(level.dimension()), this.world))
                .findFirst();
    }

    // -----

    public void setName(String name) throws Exception {
        this.name = name;
        StorageManager.StorageSaver();
    }

    private static String getDimensionId(ResourceKey<Level> dimensionKey) {
        // ResourceKey#location() missing in this mapping; parse id from toString form
        String raw = dimensionKey.toString();
        int splitIndex = raw.indexOf("/ ");
        if (splitIndex >= 0 && raw.endsWith("]")) {
            return raw.substring(splitIndex + 2, raw.length() - 1);
        }
        return raw;
    }
}