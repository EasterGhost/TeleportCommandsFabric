package org.AndrewElizabeth.teleportcommandsfabric.ui.cli;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.RecordedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminHelpRenderer;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminHelpRequest;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminHelpTopic;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminIntegrationStatus;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminModuleStatus;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminRuntimeInfo;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.admin.AdminStatusRenderer;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.back.BackPreviewRenderer;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.cache.WarpListCache;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.pagination.PageView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.SortDirection;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageAssembler;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointFilterPickerKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointFilter;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointListQuery;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPageRequest;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.WaypointPages;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.render.CommandLinkBuilder;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.WaypointSort;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.query.SortKey;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.AndrewElizabeth.teleportcommandsfabric.testsupport.ScenarioTestSupport.*;
import static org.AndrewElizabeth.teleportcommandsfabric.testsupport.TextAssertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class CliTestSuite {
	private static final ResourceKey<Level> OVERWORLD = dimension("minecraft:overworld");
	private static final ResourceKey<Level> NETHER = dimension("minecraft:the_nether");
	private static final ResourceKey<Level> END = dimension("minecraft:the_end");

	CliTestSuite() {
	}

	@TestFactory
	Stream<DynamicTest> scenarios() {
		return Stream.of(
				scenario("Command argument quoting",
						"Verify command arguments are quoted only when necessary and escaped correctly.",
						CliTestSuite::testCommandArgumentQuoting),
				scenario("Admin warps render actions and navigation",
						"Verify admin /warps output includes clickable global map state and five page candidates.",
						CliTestSuite::testAdminWarpsRenderActionsAndNavigation),
				scenario("Waypoint page picker",
						"Verify waypoint page picker renders all pages in rows and preserves the list query.",
						CliTestSuite::testWaypointPagePicker),
				scenario("Waypoint filter controls",
						"Verify waypoint filter controls render picker pages and quote dimension commands.",
						CliTestSuite::testWaypointFilterControls),
				scenario("Regular warps hide admin actions",
						"Verify regular /warps output keeps only teleport and personal map visibility controls.",
						CliTestSuite::testRegularWarpsHideAdminActions),
				scenario("Warp filter sort cache",
						"Verify warp list rows are filtered, sorted, paged, and cached by query.",
						CliTestSuite::testWarpFilterSortCache),
				scenario("Homes render markers and actions",
						"Verify /homes output shows default and temporary markers and does not show global map controls.",
						CliTestSuite::testHomesRenderMarkersAndActions),
				scenario("Back preview render",
						"Verify /back preview renders previous and death records with facing, pitch, and teleport actions.",
						CliTestSuite::testBackPreviewRender),
				scenario("Admin help render",
						"Verify /tpc help renders topic-driven overview, admin commands, config index, and config module details.",
						CliTestSuite::testAdminHelpRender),
				scenario("Admin status render",
						"Verify /tpc status output renders module states and toggle actions synchronously.",
						CliTestSuite::testAdminStatusRender));
	}

	private static void testCommandArgumentQuoting() {
		assertEquals("spawn", CommandArgumentUtils.quote("spawn"), "simple argument should not be quoted");
		assertEquals("\"minecraft:overworld\"", CommandArgumentUtils.quote("minecraft:overworld"),
				"Brigadier string arguments with namespace separators should be quoted");
		assertEquals("\"main base\"", CommandArgumentUtils.quote("main base"), "argument with spaces should be quoted");
		assertEquals("\"a\\\"b\"", CommandArgumentUtils.quote("a\"b"), "quote should be escaped");
		assertEquals("\"a\\\\b\"", CommandArgumentUtils.quote("a\\b"), "backslash should be escaped");
		assertEquals("\"\"", CommandArgumentUtils.quote(null), "null should serialize as an empty quoted argument");
	}

	private static void testAdminWarpsRenderActionsAndNavigation() {
		TestLocation spawn = location("spawn", 0, 64.0D, 0, OVERWORLD, true, 0);
		TestLocation netherHub = location("nether_hub", 120, 70.0D, -40, NETHER, false, 1);
		List<NamedLocationView> locations = new ArrayList<>();
		locations.add(spawn);
		locations.add(netherHub);
		for (int i = 2; i < 28; i++) {
			locations.add(location("warp_" + i, i, 65.0D, i * 2, i % 2 == 0 ? OVERWORLD : END, true, i));
		}

		WaypointPageRequest zhRequest = new WaypointPageRequest(
				WaypointPageKind.WARPS,
				locations,
				Set.of(netherHub.getUuid()),
				null,
				true,
				WaypointListQuery.defaultQuery(),
				"zh_cn");
		String text = render(zhRequest);
		debugBlock("ADMIN WARPS PAGE WITH 28 WARPS (en_us)", render(new WaypointPageRequest(
				WaypointPageKind.WARPS,
				locations,
				Set.of(netherHub.getUuid()),
				null,
				true,
				WaypointListQuery.defaultQuery(),
				"en_us")));

		assertContains(text, "========== 传送点 (第 1/7 页) ==========", "header should show page 1 of 7");
		assertContains(text, "  - spawn [地图: 开] [全局地图: 开]\n     | [X0 Y64 Z0] [minecraft:overworld]\n     | [传送] [重命名] [更新位置] [删除] [地图隐藏] ",
				"admin visible warp should show global map state on the name line");
		assertContains(text, "  - nether_hub [地图: 关] [全局地图: 关]\n     | [X120 Y70 Z-40] [minecraft:the_nether]\n     | [传送] [重命名] [更新位置] [删除] [地图显示] ",
				"admin hidden warp should show global map state on the name line");
		assertNotContains(text, "[全局地图隐藏] ", "global map action should not be rendered on the action line");
		assertNotContains(text, "[全局地图显示] ", "global map action should not be rendered on the action line");
		assertContains(text, "[<< 首页] [< 上一页] [1] [2] [3] [4] [5] [跳页] [下一页 >] [末页 >>]",
				"navigation should render five candidate page buttons and a jump button");
	}

	private static void testWaypointPagePicker() {
		List<NamedLocationView> locations = new ArrayList<>();
		for (int i = 0; i < 34; i++) {
			locations.add(location("warp_" + i, i, 65.0D, i * 2, OVERWORLD, true, i));
		}
		WaypointListQuery query = new WaypointListQuery(2,
				WaypointFilter.dimension("minecraft:overworld"),
				new WaypointSort(SortKey.SEQUENCE, SortDirection.DESC));
		String text = renderPagePicker(new WaypointPageRequest(
				WaypointPageKind.WARPS,
				locations,
				Set.of(),
				null,
				true,
				query,
				"en_us"));

		assertContains(text, "Warps pages (2/9)", "page picker should render title and current page");
		assertContains(text, "[1] [2] [3] [4] [5] [6] [7] [8]\n[9]",
				"page picker should render eight page buttons per row");
	}

	private static void testWaypointFilterControls() {
		List<NamedLocationView> locations = List.of(
				location("alpha", 0, 64.0D, 0, OVERWORLD, true, 0),
				location("nether", 0, 64.0D, 0, NETHER, true, 1),
				location("end", 0, 64.0D, 0, END, true, 2));
		WaypointListQuery query = new WaypointListQuery(2,
				WaypointFilter.dimension("minecraft:overworld"),
				new WaypointSort(SortKey.NAME, SortDirection.ASC));
		WaypointPageRequest request = new WaypointPageRequest(
				WaypointPageKind.WARPS,
				locations,
				Set.of(),
				null,
				true,
				query,
				"en_us");
		String prefixPicker = renderFilterPicker(request, WaypointFilterPickerKind.PREFIX);
		String dimensionPicker = renderFilterPicker(request, WaypointFilterPickerKind.DIMENSION);
		CommandLinkBuilder commands = new CommandLinkBuilder();

		assertContains(prefixPicker, "Warps Prefix Filter",
				"prefix picker should render its title");
		assertContains(prefixPicker, "[All]\n[A] [B] [C] [D] [E] [F] [G] [H] [I]",
				"prefix picker should render A-Z buttons in rows");
		assertContains(dimensionPicker, "Warps Dimension Filter",
				"dimension picker should render its title");
		assertContains(dimensionPicker, "[All]\n[overworld] [the_end] [the_nether]",
				"dimension picker should render dimensions present in the current rows");
		assertContains(commands.dimensionFilterCommand(WaypointPageKind.WARPS, query, "minecraft:overworld"),
				"filter dimension \"minecraft:overworld\"",
				"dimension filter commands should quote namespaced dimension IDs");
	}

	private static void testRegularWarpsHideAdminActions() {
		TestLocation spawn = location("spawn", 0, 64.0D, 0, OVERWORLD, true, 0);
		String text = render(new WaypointPageRequest(
				WaypointPageKind.WARPS,
				List.of(spawn),
				Set.of(),
				null,
				false,
				WaypointListQuery.defaultQuery(),
				"zh_cn"));

		assertContains(text, "     | [传送] [地图隐藏] ", "regular player should see teleport and personal map action");
		assertNotContains(text, "[重命名]", "regular player should not see rename");
		assertNotContains(text, "[更新位置]", "regular player should not see update");
		assertNotContains(text, "[删除]", "regular player should not see delete");
		assertNotContains(text, "[全局地图隐藏]", "regular player should not see global map action");
	}

	private static void testWarpFilterSortCache() {
		List<NamedLocationView> rows = List.of(
				location("alpha", 0, 64.0D, 0, OVERWORLD, true, 2),
				location("atlas", 0, 64.0D, 0, NETHER, true, 1),
				location("beta", 0, 64.0D, 0, OVERWORLD, true, 0),
				location("argon", 0, 64.0D, 0, END, true, 3));
		WaypointListQuery query = new WaypointListQuery(1,
				WaypointFilter.prefix("a"),
				new WaypointSort(SortKey.NAME, SortDirection.DESC));
		WaypointPageRequest request = new WaypointPageRequest(
				WaypointPageKind.WARPS,
				rows,
				Set.of(),
				null,
				false,
				query,
				"en_us");
		WarpListCache cache = new WarpListCache();
		WaypointPageAssembler assembler = new WaypointPageAssembler(cache);

		PageView<NamedLocationView> first = assembler.page(request);
		PageView<NamedLocationView> second = assembler.page(request);

		assertEquals(List.of("atlas", "argon", "alpha"), first.entries().stream().map(NamedLocationView::getName).toList(),
				"prefix filter and descending name sort should produce expected row order");
		assertEquals(first.entries(), second.entries(), "cached result should be stable for repeated request");
		assertEquals(1, cache.cachedQueryCount(), "warp cache should hold one query result");
	}

	private static void testHomesRenderMarkersAndActions() {
		TestLocation home = location("main home", 10, 64.0D, 20, OVERWORLD, true, 0);
		TestLocation temporary = temporaryLocation("temp", 30, 70.0D, 40, NETHER, true, 1);
		String text = render(new WaypointPageRequest(
				WaypointPageKind.HOMES,
				List.of(home, temporary),
				Set.of(),
				home.getUuid(),
				true,
				WaypointListQuery.defaultQuery(),
				"zh_cn"));

		assertContains(text, "  - main home (默认) [地图: 开]", "default home marker should render");
		assertContains(text, "  - temp [临时] [地图: 开]", "temporary home marker should render");
		assertNotContains(text, "[全局地图", "home page should not show global map controls");
	}

	private static void testBackPreviewRender() {
		BackPreviewRenderer renderer = new BackPreviewRenderer();
		RecordedLocationView previous = recordedLocation(120, 64, -35, OVERWORLD, 90.0F, 12.5F);
		RecordedLocationView death = recordedLocation(24, 70, -96, NETHER, 180.0F, 0.0F);

		String text = renderer.render("en_us", Optional.of(previous), Optional.of(death)).getString();
		assertContains(text, "Back preview:\nPrevious teleport location:\nWorld: minecraft:overworld\nPosition: 120 64 -35\nFacing: West (90.0°)\nPitch: 12.5°\n[Teleport]",
				"preview should render previous teleport location with west facing and teleport action");
		assertContains(text, "Previous death location:\nWorld: minecraft:the_nether\nPosition: 24 70 -96\nFacing: North (180.0°)\nPitch: 0.0°\n[Teleport]",
				"preview should render death location with north facing and teleport action");

		String emptyText = renderer.render("en_us", Optional.empty(), Optional.empty()).getString();
		assertContains(emptyText, "Back preview:\nNo back locations have been recorded.",
				"empty preview should render a plain no-record message");
	}

	private static void testAdminHelpRender() {
		AdminHelpRenderer renderer = new AdminHelpRenderer();
		AdminRuntimeInfo runtimeInfo = testRuntimeInfo();
		String overview = renderer.render(AdminHelpRequest.overview("en_us", runtimeInfo)).getString();
		String admin = renderer.render(AdminHelpRequest.admin("en_us", runtimeInfo)).getString();
		String config = renderer.render(AdminHelpRequest.config("en_us", runtimeInfo)).getString();
		String homeConfig = renderer.render(AdminHelpRequest.configModule("home", "en_us", runtimeInfo)).getString();
		String zhRtpConfig = renderer.render(new AdminHelpRequest(AdminHelpTopic.CONFIG_MODULE,
				"rtp", "zh_cn", runtimeInfo)).getString();

		assertContains(overview, "========== TeleportCommandsFabric Admin ==========\nVersion: test-version\nIntegrations: Xaero loaded\nTopics:\n[Admin Commands] [Config Commands]\nQuick:\n[status] [reload] [debug] [enable] [disable]",
				"overview help should render compact topic and quick command entries");
		assertContains(admin, "========== TPC Admin Commands ==========", "admin help should render admin title");
		assertContains(admin, "/tpc enable <module>\n  Enable a command module.",
				"admin help should render command usage with description");
		assertContains(admin, "Modules:\nback home tpa warp worldspawn rtp xaero",
				"admin help should render module names");
		assertContains(config, "========== TPC Config Commands ==========\nModules:\n[teleporting] [back] [home] [tpa]\n[warp] [worldspawn] [rtp] [xaero]\n[storage]",
				"config index should render config modules as topic buttons");
		assertContains(homeConfig, "========== TPC Config: home ==========\n/tpc config home max <count>\n  Set the maximum number of homes per player.\n/tpc config home deleteInvalid <true|false>",
				"config module help should render home config commands");
		assertContains(zhRtpConfig, "========== TPC 配置：rtp ==========\n/tpc config rtp minRadius <blocks>\n  设置 RTP 最小搜索半径。",
				"config module help should localize descriptions");
	}

	private static void testAdminStatusRender() {
		AdminStatusRenderer renderer = new AdminStatusRenderer();
		List<AdminModuleStatus> modules = List.of(
				new AdminModuleStatus("home", "commands.teleport_commands.admin.module.home", true),
				new AdminModuleStatus("warp", "commands.teleport_commands.admin.module.warp", false));
		String zhText = renderer.render(modules, "zh_cn", testRuntimeInfo()).getString();
		String enText = renderer.render(modules, "en_us", testRuntimeInfo()).getString();

		assertContains(zhText, "TPC 状态：\n版本：test-version\n联动：Xaero 已加载\n模块状态：\n",
				"admin status should render runtime info and title");
		assertContains(zhText, "Home 命令：已启用 [禁用]\n", "enabled module should render disable action");
		assertContains(zhText, "Warp 命令：已禁用 [启用]\n", "disabled module should render enable action");
		assertContains(enText, "TPC status:\nVersion: test-version\nIntegrations: Xaero loaded\nModule status:\nHome command: enabled [disable]\nWarp command: disabled [enable]\n",
				"English admin status should render expected text");
		assertEquals("\n===========================", renderer.renderRefreshDivider().getString(),
				"refresh divider should match legacy status refresh separator");
	}

	private static String render(WaypointPageRequest request) {
		try (WaypointPages pages = new WaypointPages()) {
			CompletableFuture<Component> future = pages.render(request);
			return future.join().getString();
		}
	}

	private static String renderPagePicker(WaypointPageRequest request) {
		try (WaypointPages pages = new WaypointPages()) {
			return pages.renderPagePicker(request).getString();
		}
	}

	private static String renderFilterPicker(WaypointPageRequest request, WaypointFilterPickerKind pickerKind) {
		try (WaypointPages pages = new WaypointPages()) {
			return pages.renderFilterPicker(request, pickerKind).getString();
		}
	}

	private static AdminRuntimeInfo testRuntimeInfo() {
		return AdminRuntimeInfo.of("test-version", List.of(new AdminIntegrationStatus(
				"commands.teleport_commands.admin.info.integration.xaero")));
	}

	private static TestLocation location(String name, int x, double y, int z, ResourceKey<Level> dimension,
			boolean visible, int sequence) {
		return new TestLocation(UUID.randomUUID(), name, x, y, z, dimension, visible, 0L, sequence);
	}

	private static TestLocation temporaryLocation(String name, int x, double y, int z, ResourceKey<Level> dimension,
			boolean visible, int sequence) {
		return new TestLocation(UUID.randomUUID(), name, x, y, z, dimension, visible,
				System.currentTimeMillis() + 60_000L, sequence);
	}

	private static ResourceKey<Level> dimension(String id) {
		return ResourceKey.create(Registries.DIMENSION, Identifier.tryParse(id));
	}

	private record TestLocation(
			UUID getUuid,
			String getName,
			int getX,
			double getYPrecise,
			int getZ,
			ResourceKey<Level> getDimension,
			boolean isVisible,
			long getExpiredTime,
			int getSequence) implements NamedLocationView {
	}

	private static RecordedLocationView recordedLocation(int x, int y, int z, ResourceKey<Level> dimension,
			Float yRot, Float xRot) {
		return new TestRecordedLocation(new BlockPos(x, y, z), dimension, yRot, xRot);
	}

	private record TestRecordedLocation(
			BlockPos getBlockPos,
			ResourceKey<Level> getDimension,
			Float getYRot,
			Float getXRot) implements RecordedLocationView {
	}
}
