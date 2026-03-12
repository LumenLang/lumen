package dev.lumenlang.lumen.pipeline.typebinding;

import dev.lumenlang.lumen.api.type.TypeBindingMeta;
import dev.lumenlang.lumen.pipeline.language.TypeBinding;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for all {@link TypeBinding} implementations available in a Lumen script.
 *
 * <p>Type bindings are looked up by their {@link TypeBinding#id()} during pattern matching.
 * Register custom bindings with {@link #register(TypeBinding)} before compiling scripts.
 *
 * @see TypeBinding
 * @see PatternRegistry
 */
public final class TypeRegistry {
    private final Map<String, TypeBinding> map = new HashMap<>();
    private final Map<String, TypeBindingMeta> metaMap = new HashMap<>();

    /**
     * Registers a type binding, associating it with its {@link TypeBinding#id()}.
     *
     * <p>If a binding with the same ID already exists it is silently replaced.
     *
     * @param b the binding to register
     */
    public void register(@NotNull TypeBinding b) {
        map.put(b.id(), b);
    }

    /**
     * Registers documentation metadata for a type binding.
     *
     * @param id   the type binding identifier
     * @param meta the metadata to associate with the binding
     */
    public void registerMeta(@NotNull String id, @NotNull TypeBindingMeta meta) {
        metaMap.put(id, meta);
    }

    /**
     * Returns the binding registered under the given type ID, or {@code null} if not found.
     *
     * @param id the type identifier (e.g. {@code "PLAYER"}, {@code "INT"})
     * @return the matching {@link TypeBinding}, or {@code null}
     */
    public @Nullable TypeBinding get(@NotNull String id) {
        return map.get(id);
    }

    /**
     * Returns the documentation metadata for the given type ID,
     * or {@link TypeBindingMeta#EMPTY} if none was registered.
     *
     * @param id the type identifier
     * @return the metadata, never null
     */
    public @NotNull TypeBindingMeta getMeta(@NotNull String id) {
        return metaMap.getOrDefault(id, TypeBindingMeta.EMPTY);
    }

    /**
     * Returns an unmodifiable view of all registered type bindings.
     *
     * @return map of type ID to binding
     */
    public @NotNull Map<String, TypeBinding> allBindings() {
        return Collections.unmodifiableMap(map);
    }

    /**
     * Returns an unmodifiable view of all registered type binding metadata.
     *
     * @return map of type ID to metadata
     */
    public @NotNull Map<String, TypeBindingMeta> allMeta() {
        return Collections.unmodifiableMap(metaMap);
    }
}
