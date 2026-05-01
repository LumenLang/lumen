package dev.lumenlang.lumen.pipeline.var;

import dev.lumenlang.lumen.api.codegen.TypeEnv;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.api.type.TypeUtils;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A compile-time descriptor for a named variable that is in scope during code generation.
 *
 * <p>A {@code VarRef} carries the source-level {@link #name()} for diagnostics and lookups,
 * the compile-time {@link LumenType}, and the Java variable name used in generated code.
 * For scoped globals, {@link #java()} throws because there is no single standalone
 * Java expression, callers must use {@link #globalInfo()} to build scoped accesses.
 *
 * <p>An optional {@link #metadata()} map carries additional compile-time knowledge. Metadata
 * is immutable once attached, use {@link #withMeta(String, Object)} to produce a copy.
 *
 * @param name       the source-level variable name
 * @param type       the compile-time type
 * @param java       the Java variable name, or {@code null} for scoped globals
 * @param metadata   an unmodifiable map of compile-time metadata entries
 * @param globalInfo the global declaration info, or {@code null} for locals and root variables
 * @see LumenType
 * @see TypeEnvImpl
 */
@SuppressWarnings("unused")
public record VarRef(
        @NotNull String name,
        @NotNull LumenType type,
        @Nullable String java,
        @NotNull Map<String, Object> metadata,
        @Nullable TypeEnv.GlobalInfo globalInfo
) implements TypeEnv.VarHandle {

    public VarRef(@NotNull String name, @NotNull LumenType type, @NotNull String java) {
        this(name, type, java, Map.of(), null);
    }

    public VarRef(@NotNull String name, @NotNull LumenType type, @NotNull String java, @NotNull Map<String, Object> metadata) {
        this(name, type, java, metadata, null);
    }

    @Override
    public @NotNull String java() {
        if (java == null) {
            throw new IllegalStateException("Scoped global '" + name + "' has no standalone Java expression. Build a scoped storage access using globalInfo() and an explicit scope.");
        }
        return java;
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

    public @NotNull VarRef withMeta(@NotNull String key, @NotNull Object value) {
        Map<String, Object> newMeta = new HashMap<>(metadata);
        newMeta.put(key, value);
        return new VarRef(name, type, java, Collections.unmodifiableMap(newMeta), globalInfo);
    }

    public @NotNull VarRef withType(@NotNull LumenType type) {
        return new VarRef(name, type, java, metadata, globalInfo);
    }
}
