package net.vansencool.lumen.plugin.util;

import net.vansencool.lumen.api.codegen.EnvironmentAccess.VarHandle;
import net.vansencool.lumen.api.exceptions.ParseFailureException;
import net.vansencool.lumen.api.type.RefTypeHandle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility for validating entity variable metadata at parse time.
 *
 * <p>The Java class of an entity variable is determined by:
 * <ol>
 *   <li>The {@code "javaClass"} metadata entry, if present (e.g. from mob spawn expressions)</li>
 *   <li>The variable's {@link RefTypeHandle#javaType()}, which every ref type carries
 *       (e.g. PLAYER maps to {@code org.bukkit.entity.Player})</li>
 * </ol>
 *
 * <p>Type compatibility is resolved using {@link Class#forName} and
 * {@link Class#isAssignableFrom} against the server classpath.
 */
public final class EntityValidation {

    private EntityValidation() {
    }

    /**
     * Returns the Java class name for a VarHandle.
     *
     * <p>Checks the {@code "javaClass"} metadata first, then falls back to the
     * variable's {@link RefTypeHandle#javaType()} if available.
     *
     * @param handle the variable handle
     * @return the fully-qualified java class name, or {@code null} if unknown
     */
    public static @Nullable String javaClass(@NotNull VarHandle handle) {
        Object val = handle.meta("javaClass");
        if (val instanceof String s) return s;
        RefTypeHandle refType = handle.type();
        if (refType != null) return refType.javaType();
        return null;
    }

    /**
     * Validates that the given entity variable is known to be a
     * {@code LivingEntity} at parse time.
     *
     * <p>Delegates to {@link #requireSubtype} with
     * {@code "org.bukkit.entity.LivingEntity"}.
     *
     * @param handle      the entity VarHandle
     * @param patternDesc a short description of what the pattern does (for error messages)
     * @return {@code true} if the entity is definitely a LivingEntity,
     *         {@code false} if it is unknown (safe instanceof guard needed)
     * @throws ParseFailureException if the entity is known to be incompatible
     */
    public static boolean requireLivingEntity(@NotNull VarHandle handle,
                                              @NotNull String patternDesc) {
        return requireSubtype(handle, "org.bukkit.entity.LivingEntity", patternDesc);
    }

    /**
     * Validates that the given entity variable is known to be an instance of the
     * specified Bukkit entity interface at parse time.
     *
     * <p>Resolution rules:
     * <ol>
     *   <li>No {@code javaClass} metadata -- returns {@code false} (unknown,
     *       caller should emit an {@code instanceof} guard).</li>
     *   <li>Known type equals or <em>extends</em> the required type -- returns
     *       {@code true} (safe to cast directly).</li>
     *   <li>Known type is a <em>supertype</em> of the required type (e.g.
     *       {@code LivingEntity} when {@code Zombie} is required) -- returns
     *       {@code false} (could match at runtime, emit instanceof guard).</li>
     *   <li>Types are on incompatible branches -- throws
     *       {@link ParseFailureException}.</li>
     * </ol>
     *
     * @param handle        the entity VarHandle
     * @param requiredClass the fully-qualified class name that must be assignable
     * @param patternDesc   a short description for error messages
     * @return {@code true} if the entity is known to satisfy the requirement,
     *         {@code false} if unknown (instanceof guard needed)
     * @throws ParseFailureException if the entity is known to be incompatible
     */
    public static boolean requireSubtype(@NotNull VarHandle handle,
                                         @NotNull String requiredClass,
                                         @NotNull String patternDesc) {
        String cls = javaClass(handle);
        if (cls == null) return false;
        if (cls.equals(requiredClass)) return true;

        try {
            Class<?> knownType = Class.forName(cls);
            Class<?> requiredType = Class.forName(requiredClass);

            if (requiredType.isAssignableFrom(knownType)) {
                return true;
            }
            if (knownType.isAssignableFrom(requiredType)) {
                return false;
            }
            throw new ParseFailureException(
                    "'" + patternDesc + "' requires " + simpleName(requiredClass)
                            + " but the entity is known to be " + simpleName(cls));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static @NotNull String simpleName(@NotNull String fqcn) {
        int dot = fqcn.lastIndexOf('.');
        return dot >= 0 ? fqcn.substring(dot + 1) : fqcn;
    }
}
