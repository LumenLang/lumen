package dev.lumenlang.lumen.plugin.scripts;

import dev.lumenlang.lumen.api.LumenProvider;
import dev.lumenlang.lumen.api.bus.events.AllScriptsLoadedEvent;
import dev.lumenlang.lumen.api.bus.events.ScriptLoadEvent;
import dev.lumenlang.lumen.api.bus.events.ScriptUnloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Posts script lifecycle events on the Lumen event bus.
 */
final class ScriptManagerEvents {

    private ScriptManagerEvents() {
    }

    static void postScriptLoaded(@NotNull String scriptName) {
        LumenProvider.bus().post(new ScriptLoadEvent(scriptName));
    }

    static void postScriptUnloaded(@NotNull String scriptName) {
        LumenProvider.bus().post(new ScriptUnloadEvent(scriptName));
    }

    static void postAllScriptsLoaded(@NotNull List<String> scriptNames) {
        LumenProvider.bus().post(new AllScriptsLoadedEvent(scriptNames));
    }
}
