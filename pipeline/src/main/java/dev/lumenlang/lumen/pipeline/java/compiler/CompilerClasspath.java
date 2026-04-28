package dev.lumenlang.lumen.pipeline.java.compiler;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for compiler classpath configuration shared across the pipeline.
 *
 * <p>Addons and the addon manager use this to register extra JAR paths that the
 * active compiler backend should include when compiling scripts.
 */
public final class CompilerClasspath {

    private static volatile boolean reduceClasspath;
    private static final Set<String> extraEntries = ConcurrentHashMap.newKeySet();

    /**
     * Registers an additional path to include on the script compilation classpath.
     *
     * @param path the absolute file path to add
     */
    public static void addEntry(@NotNull String path) {
        extraEntries.add(path);
    }

    /**
     * Removes a previously registered extra classpath entry.
     *
     * @param path the absolute file path to remove
     */
    public static void removeEntry(@NotNull String path) {
        extraEntries.remove(path);
    }

    /**
     * Returns an immutable snapshot of all registered extra classpath entries.
     *
     * @return the extra classpath entries
     */
    public static @NotNull Set<String> entries() {
        return Set.copyOf(extraEntries);
    }

    /**
     * Enables or disables reduced classpath mode.
     *
     * @param enabled whether to strip irrelevant packages from the classpath
     */
    public static void setReduceClasspath(boolean enabled) {
        reduceClasspath = enabled;
    }

    /**
     * Returns whether reduced classpath mode is enabled.
     *
     * @return true if reduced classpath is active
     */
    public static boolean reduceClasspath() {
        return reduceClasspath;
    }
}
