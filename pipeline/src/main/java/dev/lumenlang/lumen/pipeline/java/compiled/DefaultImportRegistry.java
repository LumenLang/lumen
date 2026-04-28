package dev.lumenlang.lumen.pipeline.java.compiled;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Holds the set of fully qualified class names that are injected as imports into every compiled script class.
 */
public final class DefaultImportRegistry {

    private static final Set<String> imports = new LinkedHashSet<>();

    private DefaultImportRegistry() {
    }

    /**
     * Registers a fully qualified class name as a default import.
     *
     * @param fullyQualifiedName the class name to register
     */
    public static void register(@NotNull String fullyQualifiedName) {
        imports.add(fullyQualifiedName);
    }

    /**
     * Removes a previously registered default import.
     *
     * @param fullyQualifiedName the class name to remove
     */
    public static void unregister(@NotNull String fullyQualifiedName) {
        imports.remove(fullyQualifiedName);
    }

    /**
     * Returns an unmodifiable view of all registered default imports.
     *
     * @return the registered imports
     */
    public static @NotNull Collection<String> all() {
        return Collections.unmodifiableSet(imports);
    }
}
