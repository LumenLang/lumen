package dev.lumenlang.lumen.api;

import org.jetbrains.annotations.NotNull;

/**
 * Enumerates Lumen string configuration options that addons can override.
 *
 * @see StringConfigOverride
 * @see LumenAddon#stringConfigOverrides()
 */
public enum StringConfigOption {

    /**
     * Controls which Java compiler backend is used to compile scripts.
     */
    COMPILER("performance.compiler");

    private final String path;

    StringConfigOption(@NotNull String path) {
        this.path = path;
    }

    /**
     * Returns the full dot separated path to this option in {@code config.yml}.
     *
     * @return the config path
     */
    public @NotNull String path() {
        return path;
    }
}
