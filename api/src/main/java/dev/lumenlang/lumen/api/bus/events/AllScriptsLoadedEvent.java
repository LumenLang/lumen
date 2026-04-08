package dev.lumenlang.lumen.api.bus.events;

import dev.lumenlang.lumen.api.bus.LumenEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Posted after all scripts have been loaded (or reloaded) in a batch
 * operation such as startup or {@code /luma reload}.
 */
public final class AllScriptsLoadedEvent extends LumenEvent {

    private final List<String> scriptNames;

    public AllScriptsLoadedEvent(@NotNull List<String> scriptNames) {
        this.scriptNames = List.copyOf(scriptNames);
    }

    /**
     * Returns the names of all scripts that were loaded.
     *
     * @return unmodifiable list of script names
     */
    public @NotNull List<String> scriptNames() {
        return scriptNames;
    }
}
