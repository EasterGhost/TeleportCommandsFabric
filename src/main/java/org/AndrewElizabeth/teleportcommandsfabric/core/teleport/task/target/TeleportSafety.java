package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.target;

import org.AndrewElizabeth.teleportcommandsfabric.core.teleport.task.SafetyBlockRules;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

final class TeleportSafety {
    static final int SEARCH_RADIUS = 3;
    private static final int CACHE_X_SIZE = SEARCH_RADIUS * 2 + 1;
    private static final int CACHE_Y_SIZE = SEARCH_RADIUS * 2 + 3;
    private static final int CACHE_Z_SIZE = SEARCH_RADIUS * 2 + 1;
    private static final int CACHE_Y_OFFSET = SEARCH_RADIUS + 1;
    private static final byte CACHE_UNKNOWN = 0;
    private static final byte MASK_SUPPORT = 1;
    private static final byte MASK_BODY_CLEAR = 2;
    private static final BooleanSupplier NEVER_CANCELLED = () -> false;
    private static final Offset[] CANDIDATE_OFFSETS = createCandidateOffsets();
    private static final ThreadLocal<SearchContext> SEARCH_CONTEXT = ThreadLocal.withInitial(SearchContext::new);

    private TeleportSafety() {
    }

    public static Optional<BlockPos> getSafeBlockPos(BlockPos blockPos, ServerLevel world) {
        return getSafeBlockPos(blockPos, world, world::getBlockState);
    }

    public static Optional<BlockPos> getSafeBlockPos(BlockPos blockPos, ServerLevel world, BlockStateReader reader) {
        return getSafeBlockPos(blockPos, world, reader, NEVER_CANCELLED);
    }

    static Optional<BlockPos> getSafeBlockPos(BlockPos blockPos, ServerLevel world, BlockStateReader reader,
            BooleanSupplier cancellationRequested) {
        SearchContext context = SEARCH_CONTEXT.get();
        context.reset(blockPos, world, reader, cancellationRequested);
        try {
            for (Offset offset : CANDIDATE_OFFSETS) {
                context.throwIfCancelled();
                if (context.isSafe(offset)) {
                    return Optional.of(context.toBlockPos(offset));
                }
            }
            return Optional.empty();
        } finally {
            context.clearWorld();
        }
    }

    @FunctionalInterface
    public interface BlockStateReader {
        BlockState getBlockState(BlockPos pos);
    }

    private static Offset[] createCandidateOffsets() {
        List<Offset> offsets = new ArrayList<>();
        for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
            for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
                for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++) {
                    if (x == 0 && y == 0 && z == 0) {
                        offsets.add(new Offset(x, y, z));
                        continue;
                    }
                    if (Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z)) <= SEARCH_RADIUS) {
                        offsets.add(new Offset(x, y, z));
                    }
                }
            }
        }

        offsets.sort(Comparator
                .comparingInt((Offset offset) -> yPriority(offset.y()))
                .thenComparingInt(Offset::horizontalDistanceSquared)
                .thenComparingInt(Offset::distanceSquared)
                .thenComparingInt(Offset::z)
                .thenComparingInt(Offset::x));
        return offsets.toArray(Offset[]::new);
    }

    private static int yPriority(int y) {
        if (y == 0) {
            return 0;
        }
        return Math.abs(y) * 2 - (y > 0 ? 1 : 0);
    }

    private static final class SearchContext {
        private int baseX;
        private int baseY;
        private int baseZ;
        private ServerLevel world;
        private BlockStateReader reader;
        private BooleanSupplier cancellationRequested;
        private final byte[] maskCache = new byte[CACHE_X_SIZE * CACHE_Y_SIZE * CACHE_Z_SIZE];
        private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        private void reset(BlockPos blockPos, ServerLevel world, BlockStateReader reader,
                BooleanSupplier cancellationRequested) {
            this.baseX = blockPos.getX();
            this.baseY = blockPos.getY();
            this.baseZ = blockPos.getZ();
            this.world = world;
            this.reader = reader;
            this.cancellationRequested = cancellationRequested;
            Arrays.fill(maskCache, CACHE_UNKNOWN);
        }

        private void clearWorld() {
            this.world = null;
            this.reader = null;
            this.cancellationRequested = null;
        }

        private boolean isSafe(Offset offset) {
            if (!hasMask(offset.x(), offset.y() - 1, offset.z(), MASK_SUPPORT)) {
                return false;
            }
            if (!hasMask(offset.x(), offset.y(), offset.z(), MASK_BODY_CLEAR)) {
                return false;
            }
            return hasMask(offset.x(), offset.y() + 1, offset.z(), MASK_BODY_CLEAR);
        }

        private boolean hasMask(int relativeX, int relativeY, int relativeZ, byte requiredMask) {
            return (getMask(relativeX, relativeY, relativeZ) & requiredMask) != 0;
        }

        private byte getMask(int relativeX, int relativeY, int relativeZ) {
            int index = cacheIndex(relativeX, relativeY, relativeZ);
            byte cached = maskCache[index];
            if (cached != CACHE_UNKNOWN) {
                return (byte) (cached - 1);
            }

            mutablePos.set(baseX + relativeX, baseY + relativeY, baseZ + relativeZ);
            throwIfCancelled();
            BlockState state = reader.getBlockState(mutablePos);
            throwIfCancelled();
            byte mask = createMask(state);
            maskCache[index] = (byte) (mask + 1);
            return mask;
        }

        private byte createMask(BlockState state) {
            if (state.isAir() || SafetyBlockRules.isDoor(state.getBlock())) {
                return MASK_BODY_CLEAR;
            }
            if (state.is(Blocks.WATER)) {
                return MASK_SUPPORT | MASK_BODY_CLEAR;
            }

            throwIfCancelled();
            boolean collisionEmpty = state.getCollisionShape(world, mutablePos).isEmpty();
            throwIfCancelled();
            byte mask = 0;

            if (!collisionEmpty && !SafetyBlockRules.isUnsafeSupport(state.getBlock())) {
                mask |= MASK_SUPPORT;
            }
            if (collisionEmpty && !SafetyBlockRules.isUnsafeCollisionFreeBlock(state.getBlock())) {
                mask |= MASK_BODY_CLEAR;
            }

            return mask;
        }

        private void throwIfCancelled() {
            if (cancellationRequested.getAsBoolean()) {
                throw new CancellationException("Teleport safety check cancelled");
            }
        }

        private int cacheIndex(int relativeX, int relativeY, int relativeZ) {
            int x = relativeX + SEARCH_RADIUS;
            int y = relativeY + CACHE_Y_OFFSET;
            int z = relativeZ + SEARCH_RADIUS;
            return (y * CACHE_Z_SIZE + z) * CACHE_X_SIZE + x;
        }

        private BlockPos toBlockPos(Offset offset) {
            return new BlockPos(baseX + offset.x(), baseY + offset.y(), baseZ + offset.z());
        }
    }

    private record Offset(int x, int y, int z) {
        private int horizontalDistanceSquared() {
            return x * x + z * z;
        }

        private int distanceSquared() {
            return x * x + y * y + z * z;
        }
    }
}
