package net.vansencool.lumen.pipeline.var;

import net.vansencool.lumen.api.codegen.EnvironmentAccess;
import net.vansencool.lumen.api.type.RefTypeHandle;
import net.vansencool.lumen.api.type.TypeHandle;
import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.type.LumenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A compile-time descriptor for a named variable that is in scope during code generation.
 *
 * <p>A {@code VarRef} represents knowledge that, at runtime, there will be a local variable
 * bound to the given Java name. If a {@link RefType} is present, the variable participates
 * in implicit resolution (e.g. a player reference can be used implicitly by type bindings
 * when no explicit token is provided in script source).
 *
 * <p>An optional {@link #lumenType()} carries the full compile-time type, covering both
 * primitives and object reference types. When present, {@link #refType()} is derived from it.
 *
 * <p>An optional {@link #metadata()} map carries additional compile-time knowledge about the
 * variable. For example, an entity variable might carry {@code {"entityType": "ZOMBIE"}} so
 * that downstream statements and conditions can generate type-specific code. Metadata is
 * immutable once attached; use {@link #withMeta(String, Object)} to produce a copy with
 * additional entries.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // A typed variable (player) that can be resolved implicitly:
 * env.defineVar("player", new VarRef(RefType.PLAYER, "player"));
 *
 * // A plain variable (args) with no implicit resolution:
 * env.defineVar("args", new VarRef(null, "args"));
 *
 * // A variable with full type and metadata:
 * VarRef mob = new VarRef(RefType.ENTITY, "mob", LumenType.fromHandle(Types.ENTITY))
 *     .withMeta("entityType", "ZOMBIE");
 * }</pre>
 *
 * @param refType   the logical type category for implicit resolution, or {@code null} for plain variables
 * @param java      the Java variable name that will appear in generated source
 * @param lumenType the full compile-time type, or {@code null} if unknown
 * @param metadata  an unmodifiable map of compile-time metadata entries
 * @see RefType
 * @see LumenType
 * @see TypeEnv
 */
@SuppressWarnings("unused")
public record VarRef(@Nullable RefType refType, @NotNull String java,
                     @Nullable LumenType lumenType, @NotNull Map<String, Object> metadata)
        implements EnvironmentAccess.VarHandle {

    /**
     * Creates a {@code VarRef} with no metadata and no explicit LumenType.
     *
     * @param refType the logical type category, or {@code null}
     * @param java    the Java variable name
     */
    public VarRef(@Nullable RefType refType, @NotNull String java) {
        this(refType, java, refType != null ? new LumenType.ObjectType(refType) : null, Map.of());
    }

    /**
     * Creates a {@code VarRef} with metadata but no explicit LumenType.
     *
     * @param refType  the logical type category, or {@code null}
     * @param java     the Java variable name
     * @param metadata compile-time metadata entries
     */
    public VarRef(@Nullable RefType refType, @NotNull String java, @NotNull Map<String, Object> metadata) {
        this(refType, java, refType != null ? new LumenType.ObjectType(refType) : null, metadata);
    }

    /**
     * Creates a {@code VarRef} with a LumenType and no metadata.
     *
     * @param refType   the logical type category, or {@code null}
     * @param java      the Java variable name
     * @param lumenType the full compile-time type, or {@code null}
     */
    public VarRef(@Nullable RefType refType, @NotNull String java, @Nullable LumenType lumenType) {
        this(refType, java, lumenType, Map.of());
    }

    /**
     * Returns the resolved compile-time type, computing it from the ref type if needed.
     *
     * @return the resolved type, or {@code null} if no type information is available
     */
    public @Nullable LumenType resolvedType() {
        if (lumenType != null) return lumenType;
        if (refType != null) return new LumenType.ObjectType(refType);
        return null;
    }

    @Override
    public @Nullable RefTypeHandle type() {
        return refType;
    }

    @Override
    public @Nullable TypeHandle typeHandle() {
        return resolvedType();
    }

    /**
     * Returns the metadata value for the given key, or {@code null} if absent.
     *
     * @param key the metadata key
     * @return the value, or {@code null}
     */
    @Override
    public @Nullable Object meta(@NotNull String key) {
        return metadata.get(key);
    }

    /**
     * Returns {@code true} if metadata contains the given key.
     *
     * @param key the metadata key
     * @return {@code true} if present
     */
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
        return new VarRef(refType, java, lumenType, Collections.unmodifiableMap(newMeta));
    }

    /**
     * Returns a new {@code VarRef} that is identical but with the given LumenType.
     *
     * @param type the new compile-time type
     * @return a new {@code VarRef} with the given type
     */
    public @NotNull VarRef withType(@NotNull LumenType type) {
        return new VarRef(refType, java, type, metadata);
    }
}
