package dev.lumenlang.lumen.pipeline.persist;

import dev.lumenlang.lumen.pipeline.persist.impl.FilePersistentStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Strategy interface for persistent variable storage backends.
 *
 * <p>Implementations of this interface determine how persistent variables are serialized
 * and stored across server restarts. The default implementation uses local file storage
 * with binary serialization, but addons can replace it with database-backed storage
 * (e.g. MySQL, Redis, SQLite) via {@link PersistentVars#setStorage(PersistentStorage)}.
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>Keys are dot-separated strings like {@code "ScriptName.varName"}</li>
 *   <li>{@link #get(String)} returns {@code null} if the key does not exist</li>
 *   <li>{@link #set(String, Object)} persists immediately (or as quickly as the backend allows)</li>
 *   <li>Values can be any type supported by the active serializer</li>
 *   <li>{@link #flush()} ensures all pending writes are committed</li>
 * </ul>
 *
 * @see PersistentVars
 * @see FilePersistentStorage
 */
public interface PersistentStorage {

    /**
     * Retrieves a stored value by key.
     *
     * @param key the variable key
     * @return the stored value, or {@code null} if not present
     */
    @Nullable Object get(@NotNull String key);

    /**
     * Stores a value under the given key, persisting it.
     *
     * @param key   the variable key
     * @param value the value to store
     */
    void set(@NotNull String key, @NotNull Object value);

    /**
     * Removes a stored value.
     *
     * @param key the variable key
     */
    void delete(@NotNull String key);

    /**
     * Removes all stored values whose keys start with the given prefix.
     *
     * @param prefix the key prefix to match
     */
    default void deleteByPrefix(@NotNull String prefix) {
    }

    /**
     * Flushes any pending writes to the backing store.
     *
     * <p>Called during server shutdown to ensure all data is saved.
     */
    void flush();

    /**
     * Loads the backing store into memory. Called once during initialization.
     */
    void load();
}
