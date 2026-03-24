package dev.lumenlang.lumen.pipeline.var;

import dev.lumenlang.lumen.api.type.RefTypeHandle;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Describes a logical Bukkit reference type that Lumen can track at compile time.
 *
 * <p>Each instance carries a unique identifier and the fully-qualified Java class name for the
 * corresponding Bukkit type. This information is used during code generation to emit correct
 * type declarations and, when needed, to automatically produce import statement.
 *
 * <pre>{@code
 * RefType BLOCK = RefType.register("BLOCK", "org.bukkit.block.Block");
 * }</pre>
 */
@SuppressWarnings("unused")
public final class RefType implements RefTypeHandle {

    private static final Map<String, RefType> BY_ID = new ConcurrentHashMap<>();
    private static final Map<String, RefType> BY_JAVA = new ConcurrentHashMap<>();

    public static final RefType PLAYER = register("PLAYER", "org.bukkit.entity.Player", "$.getUniqueId().toString()");
    public static final RefType SENDER = register("SENDER", "org.bukkit.command.CommandSender", "$.getName()");
    public static final RefType LOCATION = register("LOCATION", "org.bukkit.Location");
    public static final RefType ITEMSTACK = register("ITEMSTACK", "org.bukkit.inventory.ItemStack");
    public static final RefType WORLD = register("WORLD", "org.bukkit.World", "$.getName()");
    public static final RefType OFFLINE_PLAYER = register("OFFLINE_PLAYER", "org.bukkit.OfflinePlayer", "$.getUniqueId().toString()");
    public static final RefType LIST = register("LIST", "java.util.List");
    public static final RefType MAP = register("MAP", "java.util.Map");
    public static final RefType ENTITY = register("ENTITY", "org.bukkit.entity.Entity", "$.getUniqueId().toString()");
    public static final RefType BLOCK = register("BLOCK", "org.bukkit.block.Block", "$.getLocation().toString()");
    public static final RefType DATA = register("DATA", "dev.lumenlang.lumen.pipeline.java.compiled.DataInstance");

    /**
     * The unique identifier (e.g. {@code "PLAYER"}).
     */
    public final String id;

    /**
     * The fully-qualified Java class name for this reference type.
     */
    public final String javaType;

    /**
     * A template for generating a unique key expression from a variable of this type.
     *
     * <p>The placeholders {@code $} is replaced with the actual Java variable name at
     * code generation time. For example, the PLAYER type uses
     * {@code "$.getUniqueId().toString()"}, which produces
     * {@code "player.getUniqueId().toString()"} when the variable is named {@code player}.
     *
     * <p>Defaults to {@code "String.valueOf($)"} for types that do not specify a template.
     */
    public final String keyTemplate;

    private RefType(@NotNull String id, @NotNull String javaType, @NotNull String keyTemplate) {
        this.id = id;
        this.javaType = javaType;
        this.keyTemplate = keyTemplate;
    }

    /**
     * Registers a new {@code RefType} with a default key template of {@code "String.valueOf($)"}.
     *
     * @param id       a unique identifier (e.g. {@code "BLOCK"})
     * @param javaType the fully-qualified Java class name
     * @return the registered {@code RefType} (may be the pre-existing one)
     * @see #register(String, String, String)
     */
    public static @NotNull RefType register(@NotNull String id, @NotNull String javaType) {
        return register(id, javaType, "String.valueOf($)");
    }

    /**
     * Registers a new {@code RefType} or returns the existing one if the id is already taken.
     *
     * <p>If the id is already registered with the same {@code javaType}, the existing instance
     * is returned. If the id is already registered with a different {@code javaType}, a warning
     * is logged and the existing instance is returned unchanged.
     *
     * @param id          a unique identifier (e.g. {@code "BLOCK"})
     * @param javaType    the fully-qualified Java class name
     * @param keyTemplate a template for producing a unique runtime key from a variable of this
     *                    type; the placeholders {@code $} is replaced with the Java variable name
     * @return the registered {@code RefType} (may be the pre-existing one)
     */
    public static @NotNull RefType register(@NotNull String id, @NotNull String javaType, @NotNull String keyTemplate) {
        RefType existing = BY_ID.get(id);
        if (existing != null) {
            if (!existing.javaType.equals(javaType)) {
                LumenLogger.warning(
                        "RefType '" + id + "' is already registered with javaType '" + existing.javaType
                                + "', ignoring re-registration with '" + javaType + "'"
                );
            }
            return existing;
        }
        RefType ref = new RefType(id, javaType, keyTemplate);
        BY_ID.put(id, ref);
        BY_JAVA.put(javaType, ref);
        return ref;
    }

    /**
     * Returns the {@code RefType} registered under the given id, or {@code null} if not found.
     *
     * @param id the identifier to look up
     * @return the matching {@code RefType}, or {@code null}
     */
    public static @Nullable RefType byId(@NotNull String id) {
        return BY_ID.get(id);
    }

    /**
     * Returns the {@code RefType} whose {@link #javaType} equals the given fully-qualified name,
     * or {@code null} if no match is found.
     *
     * @param javaType the fully-qualified Java class name to look up
     * @return the matching {@code RefType}, or {@code null}
     */
    public static @Nullable RefType fromJava(@NotNull String javaType) {
        return BY_JAVA.get(javaType);
    }

    /**
     * Returns an unmodifiable view of all registered {@code RefType} instances.
     *
     * @return all registered ref types
     */
    public static @NotNull Collection<RefType> values() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    @Override
    public @NotNull String id() {
        return id;
    }

    @Override
    public @NotNull String javaType() {
        return javaType;
    }

    /**
     * Returns a Java expression that produces a unique string key for a variable of this type.
     *
     * <p>The {@code $} placeholders in {@link #keyTemplate} is replaced with the given
     * Java variable name.
     *
     * @param javaVar the Java variable name to substitute
     * @return a Java expression evaluating to a unique string key
     */
    public @NotNull String keyExpression(@NotNull String javaVar) {
        return keyTemplate.replace("$", javaVar);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefType other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public @NotNull String toString() {
        return id;
    }
}
