package dev.lumenlang.lumen.plugin.defaults.util;

import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.bus.events.AllScriptsLoadedEvent;
import dev.lumenlang.lumen.api.bus.events.ScriptLoadEvent;
import dev.lumenlang.lumen.api.bus.Subscribe;
import dev.lumenlang.lumen.plugin.Lumen;
import dev.lumenlang.lumen.plugin.util.InventoryHotReload;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Subscribes to script lifecycle events and triggers inventory hot reload.
 *
 * <p>Multiple {@link ScriptLoadEvent}s and an {@link AllScriptsLoadedEvent} are
 * posted in the same tick during a batch reload. A pending task deduplicates
 * these so {@link InventoryHotReload#refreshAll()} runs exactly once per reload
 * cycle on the following tick.
 */
@Registration
@SuppressWarnings("unused")
public final class InventoryHotReloadListener {

    @Nullable
    private BukkitTask pending;

    @Subscribe
    public void onScriptLoad(@NotNull ScriptLoadEvent event) {
        scheduleRefresh();
    }

    @Subscribe
    public void onAllScriptsLoaded(@NotNull AllScriptsLoadedEvent event) {
        scheduleRefresh();
    }

    private void scheduleRefresh() {
        if (pending != null) return;
        pending = Bukkit.getScheduler().runTask(Lumen.instance(), () -> {
            pending = null;
            InventoryHotReload.refreshAll();
        });
    }
}
