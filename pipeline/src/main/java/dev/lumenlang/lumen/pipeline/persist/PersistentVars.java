package dev.lumenlang.lumen.pipeline.persist;

import dev.lumenlang.lumen.pipeline.persist.impl.FilePersistentStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Static entry point for persistent variable access from generated script code.
 *
 * <p>This class is referenced directly by compiled scripts. When a script declares
 * {@code store score default 0}, the generated Java code calls
 * {@code PersistentVars.get("ScriptName.score", 0)} to load the value and
 * {@code PersistentVars.set("ScriptName.score", value)} to save it.
 *
 * <p>For per-entity scoped storage ({@code store points for player default 0}),
 * the key includes the entity identity (e.g. player UUID).
 *
 * <h2>Storage Backend</h2>
 * <p>The default backend is {@link FilePersistentStorage}, which uses a binary file.
 * More will be added in the future, such as Redis, MongoDB. You can also implement your own by implementing the {@link PersistentStorage} interface.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>{@link #init(PersistentStorage)} is called once during Lumen startup</li>
 *   <li>Scripts call {@link #get(String, Object)} and {@link #set(String, Object)} at runtime</li>
 *   <li>{@link #shutdown()} is called during server shutdown to flush pending writes</li>
 * </ol>
 */
@SuppressWarnings("unused")
public final class PersistentVars {

    private static PersistentStorage storage;
    private static Function<Object, Object> valueResolver;

    private PersistentVars() {
    }

    /**
     * Registers a value resolver that is applied to every value returned by {@link #get(String, Object)}.
     *
     * @param resolver the resolver function, or {@code null} to remove
     */
    public static void setValueResolver(@Nullable Function<Object, Object> resolver) {
        valueResolver = resolver;
    }

    /**
     * Initializes the persistent variable system with the given storage backend.
     *
     * @param backend the storage backend to use
     */
    public static void init(@NotNull PersistentStorage backend) {
        storage = backend;
        storage.load();
    }

    /**
     * Replaces the storage backend at runtime.
     *
     * <p>The old backend is flushed before replacement. The new backend is loaded immediately.
     *
     * @param backend the new storage backend
     */
    public static void setStorage(@NotNull PersistentStorage backend) {
        if (storage != null) storage.flush();
        storage = backend;
        storage.load();
    }

    /**
     * Flushes and shuts down the persistent storage.
     */
    public static void shutdown() {
        if (storage != null) {
            storage.flush();
            storage = null;
        }
    }

    /**
     * Retrieves a persistent value, returning the default if not stored.
     *
     * <p>Called from generated script code for {@code persist var} declarations.
     *
     * @param key          the variable key (e.g. {@code "ScriptName.varName"})
     * @param defaultValue the default value if not previously stored
     * @param <T>          the value type
     * @return the stored value, or {@code defaultValue} if not present
     */
    @SuppressWarnings("unchecked")
    public static <T> @Nullable T get(@NotNull String key, @Nullable T defaultValue) {
        if (storage == null) return defaultValue;
        Object val = storage.get(key);
        if (val == null) return defaultValue;
        if (valueResolver != null) val = valueResolver.apply(val);
        return (T) val;
    }

    /**
     * Stores a persistent value.
     *
     * <p>Called from generated script code when a persistent variable is modified
     * via {@code set <var> to <value>}.
     *
     * <p>If the value is {@code null}, the entry is removed instead.
     *
     * @param key   the variable key
     * @param value the value to store, or {@code null} to remove
     */
    public static void set(@NotNull String key, @Nullable Object value) {
        if (storage == null) return;
        if (value == null) {
            delete(key);
            return;
        }
        storage.set(key, value);
    }

    /**
     * Removes a persistent value.
     *
     * @param key the variable key
     */
    public static void delete(@NotNull String key) {
        if (storage == null) return;
        storage.delete(key);
    }

    /**
     * Removes all persistent values whose keys start with the given prefix.
     *
     * @param prefix the key prefix to match
     */
    public static void deleteByPrefix(@NotNull String prefix) {
        if (storage == null) return;
        storage.deleteByPrefix(prefix);
    }

    /**
     * Returns the current storage backend, or {@code null} if not initialized.
     *
     * @return the current storage backend
     */
    public static @Nullable PersistentStorage storage() {
        return storage;
    }
}
