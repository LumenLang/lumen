package dev.lumenlang.lumen.api.imports;

import org.jetbrains.annotations.NotNull;

/**
 * Registers default Java imports added to every compiled script class.
 */
public interface ImportRegistrar {

    /**
     * Registers a fully qualified class name as a default import for all compiled scripts.
     *
     * @param fullyQualifiedName the fully qualified class name to import
     */
    void register(@NotNull String fullyQualifiedName);

    /**
     * Removes a previously registered default import.
     *
     * @param fullyQualifiedName the fully qualified class name to remove
     */
    void unregister(@NotNull String fullyQualifiedName);
}
