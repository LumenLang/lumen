package dev.lumenlang.lumen.pipeline.var;

import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.api.type.TypeUtils;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A compile-time descriptor for a named variable that is in scope during code generation.
 *
 * <p>A {@code VarRef} represents knowledge that, at runtime, there will be a local variable
 * bound to the given Java name. If a {@link LumenType} is present, the variable participates
 * in type checking (e.g. a player reference can be resolved by type bindings
 * when an explicit variable name is provided in script source).
 *
 * <p>An optional {@link #metadata()} map carries additional compile-time knowledge about the
 * variable. For example, an entity variable might carry {@code {"entityType": "ZOMBIE"}} so
 * that downstream statements and conditions can generate type-specific code. Metadata is
 * immutable once attached; use {@link #withMeta(String, Object)} to produce a copy with
 * additional entries.
 *
 * @param type     the compile-time type
 * @param java     the Java variable name that will appear in generated source
 * @param metadata an unmodifiable map of compile-time metadata entries
 * @see LumenType
 * @see TypeEnv
 */
@SuppressWarnings("unused")
public record VarRef(@NotNull LumenType type, @NotNull String java, @NotNull Map<String, Object> metadata)
        implements EnvironmentAccess.VarHandle {

    /**
     * Creates a {@code VarRef} with no metadata.
     *
     * @param type the compile-time type
     * @param java the Java variable name
     */
    public VarRef(@NotNull LumenType type, @NotNull String java) {
        this(type, java, Map.of());
    }

    /**
     * Narrows the compile-time type to {@link ObjectType} if possible.
     *
     * @return the object type, or {@code null} if this variable's type is not an ObjectType
     */
    public @Nullable ObjectType objectType() {
        return TypeUtils.asObject(type);
    }

    @Override
    public @Nullable Object meta(@NotNull String key) {
        return metadata.get(key);
    }

    @Override
    public boolean hasMeta(@NotNull String key) {
        return metadata.containsKey(key);
    }

    /**
     * Returns a new {@code VarRef} that is identical to this one but with an additional
     * metadata entry. Existing entries are preserved; if the key already exists it is
     * overwritten.
     *
     * @param key   the metadata key
     * @param value the metadata value
     * @return a new {@code VarRef} with the added metadata
     */
    public @NotNull VarRef withMeta(@NotNull String key, @NotNull Object value) {
        Map<String, Object> newMeta = new HashMap<>(metadata);
        newMeta.put(key, value);
        return new VarRef(type, java, Collections.unmodifiableMap(newMeta));
    }

    /**
     * Returns a new {@code VarRef} that is identical but with the given LumenType.
     *
     * @param type the new compile-time type
     * @return a new {@code VarRef} with the given type
     */
    public @NotNull VarRef withType(@NotNull LumenType type) {
        return new VarRef(type, java, metadata);
    }
}
