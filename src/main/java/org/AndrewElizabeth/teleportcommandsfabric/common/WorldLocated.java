package org.AndrewElizabeth.teleportcommandsfabric.common;

import org.AndrewElizabeth.teleportcommandsfabric.utils.tools;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public interface WorldLocated {
	BlockPos getBlockPos();

	String getWorldString();

	default Optional<ServerLevel> getWorld() {
		return tools.getWorldById(getWorldString());
	}
}
