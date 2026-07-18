package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild.WildSearchRegistry.OperationKey;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.wild.WildSearchRegistry.SearchState;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.TeleportStatus;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.wild.WildRequest;
import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.types.wild.WildTeleportPending;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class WildSearchRegistryTest {
	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void oldCompletionDoesNotRemoveReplacementPlayerIndex() {
		WildSearchRegistry registry = new WildSearchRegistry();
		UUID playerUuid = UUID.randomUUID();
		WildTeleportPending first = pending(playerUuid, 1L);
		WildTeleportPending replacement = pending(playerUuid, 2L);

		SearchState firstState = registry.register(first);
		SearchState replacementState = registry.register(replacement);
		OperationKey firstKey = OperationKey.of(first);
		OperationKey replacementKey = OperationKey.of(replacement);
		registry.schedule(firstKey, 5L);
		registry.schedule(replacementKey, 6L);

		first.resultFuture().complete(TeleportStatus.CANCELLED);
		assertEquals(List.of(firstKey), registry.drainCompleted());
		assertEquals(List.of(firstKey), registry.drainScheduled(5L));
		assertSame(firstState, registry.remove(firstKey));
		assertSame(replacementState, registry.getByPlayer(playerUuid));
		assertEquals(List.of(replacementKey), registry.drainScheduled(6L));

		assertSame(replacementState, registry.remove(replacementKey));
		assertNull(registry.getByPlayer(playerUuid));
	}

	private static WildTeleportPending pending(UUID playerUuid, long sequence) {
		return new WildTeleportPending(playerUuid, sequence, 0L,
				new WildRequest(512, 4096, 0, 0L, true), BlockPos.ZERO, Level.OVERWORLD);
	}
}
