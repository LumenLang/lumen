package dev.lumenlang.lumen.plugin.defaults.util;

import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.bus.events.AllScriptsLoadedEvent;
import dev.lumenlang.lumen.api.bus.events.ScriptLoadEvent;
import dev.lumenlang.lumen.api.bus.Subscribe;
import dev.lumenlang.lumen.plugin.util.InventoryHotReload;
import org.jetbrains.annotations.NotNull;

/**
 * Subscribes to script lifecycle events and triggers inventory hot reload.
 */
@Registration
@SuppressWarnings("unused")
public final class InventoryHotReloadListener {

    @Subscribe
    public void onScriptLoad(@NotNull ScriptLoadEvent event) {
        InventoryHotReload.refreshAll();
    }

    @Subscribe
    public void onAllScriptsLoaded(@NotNull AllScriptsLoadedEvent event) {
        InventoryHotReload.refreshAll();
    }
}
