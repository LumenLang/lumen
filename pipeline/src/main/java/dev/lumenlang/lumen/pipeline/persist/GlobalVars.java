package dev.lumenlang.lumen.pipeline.persist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory runtime storage for non-persistent global variables.
 *
 * <p>This class provides the same API surface as {@link PersistentVars} but stores values
 * only in memory.
 */
@SuppressWarnings("unused")
public final class GlobalVars {

    private static final ConcurrentMap<String, Object> VALUES = new ConcurrentHashMap<>();

    private GlobalVars() {
    }

    /**
     * Retrieves a runtime global value, returning the default if not present.
     *
     * @param key          the variable key (e.g. {@code "ScriptName.varName"})
     * @param defaultValue the default value if not previously stored
     * @param <T>          the value type
     * @return the stored value, or {@code defaultValue} if not present
     */
    @SuppressWarnings("unchecked")
    public static <T> @Nullable T get(@NotNull String key, @Nullable T defaultValue) {
        Object val = VALUES.get(key);
        if (val == null) return defaultValue;
        return (T) val;
    }

    /**
     * Stores a runtime global value in memory.
     *
     * <p>If the value is {@code null}, the entry is removed instead.
     *
     * @param key   the variable key
     * @param value the value to store, or {@code null} to remove
     */
    public static void set(@NotNull String key, @Nullable Object value) {
        if (value == null) {
            delete(key);
            return;
        }
        VALUES.put(key, value);
    }

    /**
     * Removes a runtime global value.
     *
     * @param key the variable key
     */
    public static void delete(@NotNull String key) {
        VALUES.remove(key);
    }

    /**
     * Removes all runtime global values whose keys start with the given prefix.
     *
     * @param prefix the key prefix to match
     */
    public static void deleteByPrefix(@NotNull String prefix) {
        VALUES.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /**
     * Clears all runtime global values. Intended for plugin reload or shutdown.
     */
    public static void clear() {
        VALUES.clear();
    }
}
