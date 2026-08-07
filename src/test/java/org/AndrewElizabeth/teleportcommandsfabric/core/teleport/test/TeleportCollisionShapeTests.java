package org.AndrewElizabeth.teleportcommandsfabric.core.teleport.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TeleportCollisionShapeTests {
	private static final double PLAYER_MIN = 0.2D;
	private static final double PLAYER_MAX = 0.8D;
	private static final double EPSILON = 1.0E-7D;
	private static final AABB PLAYER_BODY_WHEN_FEET_ABOVE_SUPPORT =
			new AABB(PLAYER_MIN, 1.0D, PLAYER_MIN, PLAYER_MAX, 2.8D, PLAYER_MAX);

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void reportsSpecialCollisionShapesAgainstCurrentSafetyRules() {
		List<Case> cases = List.of(
				new Case("stone", Blocks.STONE.defaultBlockState()),
				new Case("oak_fence", Blocks.OAK_FENCE.defaultBlockState()),
				new Case("cobblestone_wall", Blocks.COBBLESTONE_WALL.defaultBlockState()),
				new Case("closed_oak_fence_gate", Blocks.OAK_FENCE_GATE.defaultBlockState()
						.setValue(FenceGateBlock.OPEN, false)),
				new Case("open_oak_fence_gate", Blocks.OAK_FENCE_GATE.defaultBlockState()
						.setValue(FenceGateBlock.OPEN, true)),
				new Case("closed_oak_door", Blocks.OAK_DOOR.defaultBlockState()
						.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
						.setValue(DoorBlock.FACING, Direction.NORTH)
						.setValue(DoorBlock.OPEN, false)),
				new Case("open_oak_door", Blocks.OAK_DOOR.defaultBlockState()
						.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
						.setValue(DoorBlock.FACING, Direction.NORTH)
						.setValue(DoorBlock.OPEN, true)),
				new Case("bottom_oak_trapdoor", Blocks.OAK_TRAPDOOR.defaultBlockState()
						.setValue(TrapDoorBlock.HALF, Half.BOTTOM)
						.setValue(TrapDoorBlock.OPEN, false)),
				new Case("top_oak_trapdoor", Blocks.OAK_TRAPDOOR.defaultBlockState()
						.setValue(TrapDoorBlock.HALF, Half.TOP)
						.setValue(TrapDoorBlock.OPEN, false)),
				new Case("open_oak_trapdoor", Blocks.OAK_TRAPDOOR.defaultBlockState()
						.setValue(TrapDoorBlock.OPEN, true)
						.setValue(TrapDoorBlock.FACING, Direction.NORTH)),
				new Case("bottom_slab", Blocks.OAK_SLAB.defaultBlockState()
						.setValue(SlabBlock.TYPE, SlabType.BOTTOM)),
				new Case("top_slab", Blocks.OAK_SLAB.defaultBlockState()
						.setValue(SlabBlock.TYPE, SlabType.TOP)),
				new Case("snow_layer_1", Blocks.SNOW.defaultBlockState()
						.setValue(SnowLayerBlock.LAYERS, 1)),
				new Case("snow_layer_8", Blocks.SNOW.defaultBlockState()
						.setValue(SnowLayerBlock.LAYERS, 8)),
				new Case("carpet", Blocks.WHITE_CARPET.defaultBlockState()));

		System.out.println();
		System.out.println("Teleport collision shape report");
		System.out.println("name,currentSupport,landingSurfaceAtY+1,bodyIntersectsAtY+1,maxY,boxes");
		for (Case testCase : cases) {
			ShapeReport report = ShapeReport.from(testCase.state());
			System.out.println(testCase.name() + ","
					+ report.currentSupportAccepted() + ","
					+ report.hasCenteredLandingSurfaceAtY1() + ","
					+ report.intersectsPlayerBodyAtY1() + ","
					+ format(report.maxY()) + ","
					+ report.boxes());
		}

		assertFalse(ShapeReport.from(Blocks.OAK_FENCE.defaultBlockState()).currentSupportAccepted());
		assertTrue(ShapeReport.from(Blocks.OAK_FENCE.defaultBlockState()).intersectsPlayerBodyAtY1());
		assertFalse(ShapeReport.from(Blocks.OAK_FENCE.defaultBlockState()).hasCenteredLandingSurfaceAtY1());
		assertFalse(ShapeReport.from(Blocks.COBBLESTONE_WALL.defaultBlockState()).currentSupportAccepted());
		assertFalse(ShapeReport.from(Blocks.OAK_FENCE_GATE.defaultBlockState()
				.setValue(FenceGateBlock.OPEN, false)).currentSupportAccepted());

		assertFalse(ShapeReport.from(Blocks.OAK_DOOR.defaultBlockState()
				.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
				.setValue(DoorBlock.FACING, Direction.NORTH)
				.setValue(DoorBlock.OPEN, false)).currentSupportAccepted());
		assertFalse(ShapeReport.from(Blocks.OAK_DOOR.defaultBlockState()
				.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER)
				.setValue(DoorBlock.FACING, Direction.NORTH)
				.setValue(DoorBlock.OPEN, false)).hasCenteredLandingSurfaceAtY1());

		assertTrue(ShapeReport.from(Blocks.OAK_TRAPDOOR.defaultBlockState()
				.setValue(TrapDoorBlock.HALF, Half.TOP)
				.setValue(TrapDoorBlock.OPEN, false)).hasCenteredLandingSurfaceAtY1());
		assertFalse(ShapeReport.from(Blocks.OAK_TRAPDOOR.defaultBlockState()
				.setValue(TrapDoorBlock.HALF, Half.TOP)
				.setValue(TrapDoorBlock.OPEN, false)).intersectsPlayerBodyAtY1());
	}

	private static boolean currentSupportAccepted(BlockState state, VoxelShape shape) {
		Block block = state.getBlock();
		if (state.isAir() || block instanceof DoorBlock) {
			return false;
		}
		if (state.is(Blocks.WATER)) {
			return true;
		}
		return !shape.isEmpty() && !isUnsafeSupport(block);
	}

	private static boolean isUnsafeSupport(Block block) {
		return block instanceof FenceBlock || block instanceof FenceGateBlock || block instanceof WallBlock;
	}

	private static boolean hasCenteredLandingSurfaceAtY1(VoxelShape shape) {
		for (AABB box : shape.toAabbs()) {
			if (Math.abs(box.maxY - 1.0D) <= EPSILON && horizontallyIntersectsPlayerFootprint(box)) {
				return true;
			}
		}
		return false;
	}

	private static boolean intersectsPlayerBodyAtY1(VoxelShape shape) {
		for (AABB box : shape.toAabbs()) {
			if (box.intersects(PLAYER_BODY_WHEN_FEET_ABOVE_SUPPORT)) {
				return true;
			}
		}
		return false;
	}

	private static boolean horizontallyIntersectsPlayerFootprint(AABB box) {
		return box.maxX > PLAYER_MIN && box.minX < PLAYER_MAX
				&& box.maxZ > PLAYER_MIN && box.minZ < PLAYER_MAX;
	}

	private static String format(double value) {
		if (Double.isNaN(value)) {
			return "empty";
		}
		return String.format(Locale.ROOT, "%.4f", value);
	}

	private record Case(String name, BlockState state) {
	}

	private record ShapeReport(boolean currentSupportAccepted, boolean hasCenteredLandingSurfaceAtY1,
			boolean intersectsPlayerBodyAtY1, double maxY, String boxes) {
		private static ShapeReport from(BlockState state) {
			VoxelShape shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
			double maxY = shape.isEmpty() ? Double.NaN : shape.max(Direction.Axis.Y);
			return new ShapeReport(TeleportCollisionShapeTests.currentSupportAccepted(state, shape),
					TeleportCollisionShapeTests.hasCenteredLandingSurfaceAtY1(shape),
					TeleportCollisionShapeTests.intersectsPlayerBodyAtY1(shape), maxY, boxes(shape));
		}

		private static String boxes(VoxelShape shape) {
			if (shape.isEmpty()) {
				return "[]";
			}
			StringBuilder builder = new StringBuilder("[");
			boolean first = true;
			for (AABB box : shape.toAabbs()) {
				if (!first) {
					builder.append(';');
				}
				first = false;
				builder.append(format(box.minX)).append(' ')
						.append(format(box.minY)).append(' ')
						.append(format(box.minZ)).append(" -> ")
						.append(format(box.maxX)).append(' ')
						.append(format(box.maxY)).append(' ')
						.append(format(box.maxZ));
			}
			return builder.append(']').toString();
		}
	}
}
