package org.AndrewElizabeth.teleportcommandsfabric.ui.cli;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.cache.WarpListCache;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.AdminHelpRequest;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.AdminHelpTopic;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.AdminModuleStatus;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.PageView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.SortDirection;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.WaypointFilter;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.WaypointListQuery;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.WaypointPageKind;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.WaypointPageRequest;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.WaypointSort;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.WaypointSortKey;
import org.AndrewElizabeth.teleportcommandsfabric.utils.CommandArgumentUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class CliTestSuite {
	private static final String SECTION_SEPARATOR = "================================================================================";
	private static final ResourceKey<Level> OVERWORLD = dimension("minecraft:overworld");
	private static final ResourceKey<Level> NETHER = dimension("minecraft:the_nether");
	private static final ResourceKey<Level> END = dimension("minecraft:the_end");

	private CliTestSuite() {
	}

	public static void main(String[] args) {
		System.out.println("CLI tests");
		run("Command argument quoting",
				"Verify command arguments are quoted only when necessary and escaped correctly.",
				CliTestSuite::testCommandArgumentQuoting);
		run("Admin warps render actions and navigation",
				"Verify admin /warps output includes clickable global map state and five page candidates.",
				CliTestSuite::testAdminWarpsRenderActionsAndNavigation);
		run("Waypoint page picker",
				"Verify waypoint page picker renders all pages in rows and preserves the list query.",
				CliTestSuite::testWaypointPagePicker);
		run("Regular warps hide admin actions",
				"Verify regular /warps output keeps only teleport and personal map visibility controls.",
				CliTestSuite::testRegularWarpsHideAdminActions);
		run("Warp filter sort cache",
				"Verify warp list rows are filtered, sorted, paged, and cached by query.",
				CliTestSuite::testWarpFilterSortCache);
		run("Homes render markers and actions",
				"Verify /homes output shows default and temporary markers and does not show global map controls.",
				CliTestSuite::testHomesRenderMarkersAndActions);
		run("Admin help render",
				"Verify /tpc help renders topic-driven overview, admin commands, config index, and config module details.",
				CliTestSuite::testAdminHelpRender);
		run("Admin status render",
				"Verify /tpc status output renders module states and toggle actions synchronously.",
				CliTestSuite::testAdminStatusRender);
		System.out.println("CLI tests passed.");
	}

	private static void testCommandArgumentQuoting() {
		requireEquals("spawn", CommandArgumentUtils.quote("spawn"), "simple argument should not be quoted");
		requireEquals("\"main base\"", CommandArgumentUtils.quote("main base"), "argument with spaces should be quoted");
		requireEquals("\"a\\\"b\"", CommandArgumentUtils.quote("a\"b"), "quote should be escaped");
		requireEquals("\"a\\\\b\"", CommandArgumentUtils.quote("a\\b"), "backslash should be escaped");
		requireEquals("\"\"", CommandArgumentUtils.quote(null), "null should serialize as an empty quoted argument");
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

		requireContains(text, "========== 传送点 (第 1/7 页) ==========", "header should show page 1 of 7");
		requireContains(text, "  - spawn [地图: 开] [全局地图: 开]\n     | [X0 Y64 Z0] [minecraft:overworld]\n     | [传送] [重命名] [更新位置] [删除] [地图隐藏] ",
				"admin visible warp should show global map state on the name line");
		requireContains(text, "  - nether_hub [地图: 关] [全局地图: 关]\n     | [X120 Y70 Z-40] [minecraft:the_nether]\n     | [传送] [重命名] [更新位置] [删除] [地图显示] ",
				"admin hidden warp should show global map state on the name line");
		requireNotContains(text, "[全局地图隐藏] ", "global map action should not be rendered on the action line");
		requireNotContains(text, "[全局地图显示] ", "global map action should not be rendered on the action line");
		requireContains(text, "[<< 首页] [< 上一页] [1] [2] [3] [4] [5] [跳页] [下一页 >] [末页 >>]",
				"navigation should render five candidate page buttons and a jump button");
	}

	private static void testWaypointPagePicker() {
		List<NamedLocationView> locations = new ArrayList<>();
		for (int i = 0; i < 34; i++) {
			locations.add(location("warp_" + i, i, 65.0D, i * 2, OVERWORLD, true, i));
		}
		WaypointListQuery query = new WaypointListQuery(2,
				WaypointFilter.dimension("minecraft:overworld"),
				new WaypointSort(WaypointSortKey.SEQUENCE, SortDirection.DESC));
		String text = renderPagePicker(new WaypointPageRequest(
				WaypointPageKind.WARPS,
				locations,
				Set.of(),
				null,
				true,
				query,
				"en_us"));

		requireContains(text, "Warps pages (2/9)", "page picker should render title and current page");
		requireContains(text, "[1] [2] [3] [4] [5] [6] [7] [8]\n[9]",
				"page picker should render eight page buttons per row");
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

		requireContains(text, "     | [传送] [地图隐藏] ", "regular player should see teleport and personal map action");
		requireNotContains(text, "[重命名]", "regular player should not see rename");
		requireNotContains(text, "[更新位置]", "regular player should not see update");
		requireNotContains(text, "[删除]", "regular player should not see delete");
		requireNotContains(text, "[全局地图隐藏]", "regular player should not see global map action");
	}

	private static void testWarpFilterSortCache() {
		List<NamedLocationView> rows = List.of(
				location("alpha", 0, 64.0D, 0, OVERWORLD, true, 2),
				location("atlas", 0, 64.0D, 0, NETHER, true, 1),
				location("beta", 0, 64.0D, 0, OVERWORLD, true, 0),
				location("argon", 0, 64.0D, 0, END, true, 3));
		WaypointListQuery query = new WaypointListQuery(1,
				WaypointFilter.prefix("a"),
				new WaypointSort(WaypointSortKey.NAME, SortDirection.DESC));
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

		requireEquals(List.of("atlas", "argon", "alpha"), first.entries().stream().map(NamedLocationView::getName).toList(),
				"prefix filter and descending name sort should produce expected row order");
		requireEquals(first.entries(), second.entries(), "cached result should be stable for repeated request");
		requireEquals(1, cache.cachedQueryCount(), "warp cache should hold one query result");
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

		requireContains(text, "  - main home (默认) [地图: 开]", "default home marker should render");
		requireContains(text, "  - temp [临时] [地图: 开]", "temporary home marker should render");
		requireNotContains(text, "[全局地图", "home page should not show global map controls");
	}

	private static void testAdminHelpRender() {
		AdminHelpRenderer renderer = new AdminHelpRenderer();
		String overview = renderer.render(AdminHelpRequest.overview("en_us", "test-version")).getString();
		String admin = renderer.render(AdminHelpRequest.admin("en_us", "test-version")).getString();
		String config = renderer.render(AdminHelpRequest.config("en_us", "test-version")).getString();
		String homeConfig = renderer.render(AdminHelpRequest.configModule("home", "en_us", "test-version")).getString();
		String zhRtpConfig = renderer.render(new AdminHelpRequest(AdminHelpTopic.CONFIG_MODULE,
				"rtp", "zh_cn", "test-version")).getString();

		requireContains(overview, "========== TeleportCommandsFabric Admin ==========\nVersion: test-version\nTopics:\n[Admin Commands] [Config Commands]\nQuick:\n[status] [reload] [enable] [disable]",
				"overview help should render compact topic and quick command entries");
		requireContains(admin, "========== TPC Admin Commands ==========", "admin help should render admin title");
		requireContains(admin, "/tpc enable <module>\n  Enable a command module.",
				"admin help should render command usage with description");
		requireContains(admin, "Modules:\nback home tpa warp worldspawn rtp xaero",
				"admin help should render module names");
		requireContains(config, "========== TPC Config Commands ==========\nModules:\n[teleporting] [back] [home] [tpa]\n[warp] [worldspawn] [rtp] [xaero]\n[storage]",
				"config index should render config modules as topic buttons");
		requireContains(homeConfig, "========== TPC Config: home ==========\n/tpc config home max <count>\n  Set the maximum number of homes per player.\n/tpc config home deleteInvalid <true|false>",
				"config module help should render home config commands");
		requireContains(zhRtpConfig, "========== TPC 配置：rtp ==========\n/tpc config rtp minRadius <blocks>\n  设置 RTP 最小搜索半径。",
				"config module help should localize descriptions");
	}

	private static void testAdminStatusRender() {
		AdminStatusRenderer renderer = new AdminStatusRenderer();
		List<AdminModuleStatus> modules = List.of(
				new AdminModuleStatus("home", "commands.teleport_commands.admin.module.home", true),
				new AdminModuleStatus("warp", "commands.teleport_commands.admin.module.warp", false));
		String zhText = renderer.render(modules, "zh_cn").getString();
		String enText = renderer.render(modules, "en_us").getString();

		requireContains(zhText, "模块状态：\n", "admin status should render title");
		requireContains(zhText, "Home 命令：已启用 [禁用]\n", "enabled module should render disable action");
		requireContains(zhText, "Warp 命令：已禁用 [启用]\n", "disabled module should render enable action");
		requireContains(enText, "Module status:\nHome command: enabled [disable]\nWarp command: disabled [enable]\n",
				"English admin status should render expected text");
		requireEquals("\n===========================", renderer.renderRefreshDivider().getString(),
				"refresh divider should match legacy status refresh separator");
	}

	private static String render(WaypointPageRequest request) {
		try (WaypointPages pages = new WaypointPages(new WarpListCache(), Runnable::run)) {
			CompletableFuture<Component> future = pages.render(request);
			return future.join().getString();
		}
	}

	private static String renderPagePicker(WaypointPageRequest request) {
		try (WaypointPages pages = new WaypointPages(new WarpListCache(), Runnable::run)) {
			return pages.renderPagePicker(request).getString();
		}
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

	private static void run(String name, String purpose, Runnable test) {
		long start = System.nanoTime();
		System.out.println();
		System.out.println(SECTION_SEPARATOR);
		System.out.println("SCENARIO START: " + name);
		System.out.println("  PURPOSE: " + purpose);
		try {
			test.run();
			System.out.println("SCENARIO PASS: " + name + " elapsedNanos=" + formatNumber(System.nanoTime() - start));
		} catch (Throwable throwable) {
			System.err.println("SCENARIO FAIL: " + name);
			throw throwable;
		}
	}

	private static void debugBlock(String title, String text) {
		System.out.println("  " + title + ":");
		String[] lines = text.split("\\R", -1);
		for (String line : lines) {
			System.out.println("    " + line);
		}
	}

	private static void requireContains(String text, String expected, String message) {
		if (!text.contains(expected)) {
			throw new AssertionError(message + "\nexpected to contain:\n" + expected + "\nactual:\n" + text);
		}
	}

	private static void requireNotContains(String text, String unexpected, String message) {
		if (text.contains(unexpected)) {
			throw new AssertionError(message + "\nunexpected:\n" + unexpected + "\nactual:\n" + text);
		}
	}

	private static void requireEquals(Object expected, Object actual, String message) {
		if (!expected.equals(actual)) {
			throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
		}
	}

	private static String formatNumber(long value) {
		return String.format("%,d", value);
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
}
