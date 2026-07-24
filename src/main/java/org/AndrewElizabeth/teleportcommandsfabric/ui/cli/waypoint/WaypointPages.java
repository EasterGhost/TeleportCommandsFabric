package org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.cache.WarpListCache;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.pagination.PageView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.render.CommandLinkBuilder;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.render.FilterPickerRenderer;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.render.ManageRenderer;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.render.PagePickerRenderer;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.waypoint.render.PageRenderer;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WaypointPages implements AutoCloseable {
	private final WaypointPageAssembler assembler;
	private final PageRenderer renderer;
	private final PagePickerRenderer pagePickerRenderer;
	private final FilterPickerRenderer filterPickerRenderer;
	private final ManageRenderer manageRenderer;
	private final Executor renderExecutor;
	private final ExecutorService ownedExecutor;

	public WaypointPages() {
		this(new WarpListCache(), createDefaultExecutor(), true);
	}

	WaypointPages(WarpListCache warpListCache, Executor renderExecutor) {
		this(warpListCache, renderExecutor, false);
	}

	private WaypointPages(WarpListCache warpListCache, Executor renderExecutor, boolean ownsExecutor) {
		this(new WaypointPageAssembler(warpListCache),
				new PageRenderer(new CommandLinkBuilder()),
				new PagePickerRenderer(new CommandLinkBuilder()),
				new FilterPickerRenderer(new CommandLinkBuilder()),
				new ManageRenderer(new CommandLinkBuilder()),
				renderExecutor,
				ownsExecutor);
	}

	private WaypointPages(WaypointPageAssembler assembler, PageRenderer renderer,
			PagePickerRenderer pagePickerRenderer, FilterPickerRenderer filterPickerRenderer, ManageRenderer manageRenderer,
			Executor renderExecutor, boolean ownsExecutor) {
		this.assembler = Objects.requireNonNull(assembler, "assembler");
		this.renderer = Objects.requireNonNull(renderer, "renderer");
		this.pagePickerRenderer = Objects.requireNonNull(pagePickerRenderer, "pagePickerRenderer");
		this.filterPickerRenderer = Objects.requireNonNull(filterPickerRenderer, "filterPickerRenderer");
		this.manageRenderer = Objects.requireNonNull(manageRenderer, "manageRenderer");
		this.renderExecutor = Objects.requireNonNull(renderExecutor, "renderExecutor");
		this.ownedExecutor = ownsExecutor && renderExecutor instanceof ExecutorService executorService ? executorService : null;
	}

	public CompletableFuture<Component> render(WaypointPageRequest request) {
		Objects.requireNonNull(request, "request");
		return CompletableFuture.supplyAsync(() -> {
			PageView<NamedLocationView> page = assembler.page(request);
			return renderer.render(request, page);
		}, renderExecutor);
	}

	public Component renderPagePicker(WaypointPageRequest request) {
		Objects.requireNonNull(request, "request");
		PageView<NamedLocationView> page = assembler.page(request);
		return pagePickerRenderer.render(request.kind(), request.query(), page.currentPage(), page.totalPages(), request.language());
	}

	public Component renderFilterPicker(WaypointPageRequest request, WaypointFilterPickerKind pickerKind) {
		Objects.requireNonNull(request, "request");
		return filterPickerRenderer.render(request, pickerKind);
	}

	public Component renderManage(WaypointPageRequest request, NamedLocationView location) {
		Objects.requireNonNull(request, "request");
		Objects.requireNonNull(location, "location");
		return manageRenderer.render(request, location);
	}

	public Component renderDeleteConfirmation(WaypointPageRequest request, NamedLocationView location) {
		Objects.requireNonNull(request, "request");
		Objects.requireNonNull(location, "location");
		return manageRenderer.renderDeleteConfirmation(request, location);
	}

	public List<NamedLocationView> filteredRows(WaypointPageRequest request) {
		Objects.requireNonNull(request, "request");
		return assembler.filteredRows(request);
	}

	public void invalidateWarpCache() {
		assembler.invalidateWarpCache();
	}

	@Override
	public void close() {
		if (ownedExecutor != null) {
			ownedExecutor.shutdown();
		}
	}

	private static ExecutorService createDefaultExecutor() {
		return Executors.newFixedThreadPool(8, runnable -> {
			Thread thread = new Thread(runnable, "tpc-cli-renderer");
			thread.setDaemon(true);
			return thread;
		});
	}
}
