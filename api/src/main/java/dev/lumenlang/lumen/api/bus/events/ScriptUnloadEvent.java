package dev.lumenlang.lumen.api.bus.events;

import dev.lumenlang.lumen.api.bus.LumenEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Posted after a script has been unloaded and its bindings have been removed.
 */
public final class ScriptUnloadEvent extends LumenEvent {

    private final String scriptName;

    public ScriptUnloadEvent(@NotNull String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Returns the file name of the unloaded script.
     *
     * @return the script name
     */
    public @NotNull String scriptName() {
        return scriptName;
    }
}
