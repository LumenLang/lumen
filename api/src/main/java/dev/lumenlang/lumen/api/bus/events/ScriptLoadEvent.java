package dev.lumenlang.lumen.api.bus.events;

import dev.lumenlang.lumen.api.bus.LumenEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Posted after a single script has been loaded or reloaded and all its
 * bindings (commands, events, schedules) are active.
 */
public final class ScriptLoadEvent extends LumenEvent {

    private final String scriptName;

    public ScriptLoadEvent(@NotNull String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Returns the file name of the loaded script (e.g. {@code "hello.luma"}).
     *
     * @return the script name
     */
    public @NotNull String scriptName() {
        return scriptName;
    }
}
