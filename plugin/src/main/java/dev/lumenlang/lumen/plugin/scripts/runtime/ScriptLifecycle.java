package dev.lumenlang.lumen.plugin.scripts.runtime;

import dev.lumenlang.lumen.api.LumenProvider;
import dev.lumenlang.lumen.api.binder.BindingMode;
import dev.lumenlang.lumen.api.bus.events.AllScriptsLoadedEvent;
import dev.lumenlang.lumen.api.bus.events.ScriptLoadEvent;
import dev.lumenlang.lumen.api.bus.events.ScriptUnloadEvent;
import dev.lumenlang.lumen.pipeline.binder.ScriptBinder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks loaded scripts and tears them down for unload or reload.
 */
public final class ScriptLifecycle {

    private static final Map<String, LoadedScript> LOADED = new ConcurrentHashMap<>();

    private ScriptLifecycle() {
    }

    public static void register(@NotNull String name, @NotNull LoadedScript script) {
        LOADED.put(name, script);
    }

    public static boolean isLoaded(@NotNull String name) {
        return LOADED.containsKey(name);
    }

    public static @NotNull List<String> loadedNames() {
        return List.copyOf(LOADED.keySet());
    }

    /**
     * Full unload. Releases every bound resource and posts the unload event.
     */
    public static void unload(@NotNull String name) {
        LoadedScript s = LOADED.remove(name);
        if (s == null) return;
        ScriptBinder.unbindAll(s.instance(), BindingMode.UNLOAD);
        postUnloaded(name);
    }

    /**
     * Teardown ahead of an incoming new version. Returns whether a script was loaded.
     */
    public static boolean tearDownForReload(@NotNull String name) {
        LoadedScript s = LOADED.remove(name);
        if (s == null) return false;
        ScriptBinder.unbindAll(s.instance(), BindingMode.RELOAD);
        return true;
    }

    public static void postLoaded(@NotNull String name) {
        LumenProvider.bus().post(new ScriptLoadEvent(name));
    }

    public static void postUnloaded(@NotNull String name) {
        LumenProvider.bus().post(new ScriptUnloadEvent(name));
    }

    public static void postAllLoaded(@NotNull List<String> names) {
        LumenProvider.bus().post(new AllScriptsLoadedEvent(names));
    }
}
