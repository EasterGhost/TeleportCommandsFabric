package org.AndrewElizabeth.teleportcommandsfabric.ui.cli;

import org.AndrewElizabeth.teleportcommandsfabric.storage.schema.NamedLocationView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.cache.WarpListCache;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.PageView;
import org.AndrewElizabeth.teleportcommandsfabric.ui.cli.model.WaypointPageRequest;

import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WaypointPages implements AutoCloseable {
	private final WaypointPageAssembler assembler;
	private final WaypointPageRenderer renderer;
	private final WaypointPagePickerRenderer pagePickerRenderer;
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
				new WaypointPageRenderer(new WaypointCommandFactory()),
				new WaypointPagePickerRenderer(new WaypointCommandFactory()),
				renderExecutor,
				ownsExecutor);
	}

	private WaypointPages(WaypointPageAssembler assembler, WaypointPageRenderer renderer,
			WaypointPagePickerRenderer pagePickerRenderer, Executor renderExecutor, boolean ownsExecutor) {
		this.assembler = Objects.requireNonNull(assembler, "assembler");
		this.renderer = Objects.requireNonNull(renderer, "renderer");
		this.pagePickerRenderer = Objects.requireNonNull(pagePickerRenderer, "pagePickerRenderer");
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
