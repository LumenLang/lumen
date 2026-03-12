package dev.lumenlang.lumen.api.type;

import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess.VarHandle;
import dev.lumenlang.lumen.api.exceptions.ParseFailureException;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Factory for creating {@link AddonTypeBinding} instances that resolve a single token
 * to a named constant of a given class, without requiring the class to be a Java enum.
 *
 * <p>This is useful for types that expose their constants as {@code public static final}
 * fields (such as registry backed types) rather than traditional enum constants.
 *
 * <p>Use {@link #of(String, Set, String)} to create a binding from an explicit set of
 * constant names, or {@link #fromStaticFields(String, Class, String)} to discover them
 * via reflection.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * api.types().register(RegistryTypeBinding.of(
 *     "VILLAGER_TYPE",
 *     Set.of("DESERT", "JUNGLE", "PLAINS", "SAVANNA", "SNOW", "SWAMP", "TAIGA"),
 *     "org.bukkit.entity.Villager.Type"));
 * }</pre>
 *
 * @see AddonTypeBinding
 * @see TypeRegistrar
 * @see EnumTypeBinding
 */
public final class RegistryTypeBinding {

    private RegistryTypeBinding() {
    }

    /**
     * Creates an {@link AddonTypeBinding} that resolves a single token to a named constant.
     *
     * <p>The token is normalized to {@code UPPER_CASE} with spaces and hyphens replaced by
     * underscores. The generated Java code uses the short class name (imported via
     * {@link CodegenAccess#addImport(String)}).
     *
     * @param typeId        the unique type binding identifier (e.g. {@code "VILLAGER_TYPE"})
     * @param constantNames the set of valid constant names in {@code UPPER_CASE} format
     * @param fqcn          the fully qualified class name used for imports
     * @return a ready to register type binding
     */
    public static @NotNull AddonTypeBinding of(
            @NotNull String typeId,
            @NotNull Set<String> constantNames,
            @NotNull String fqcn) {
        Set<String> frozen = Set.copyOf(constantNames);
        String simpleName = fqcn.contains(".")
                ? fqcn.substring(fqcn.lastIndexOf('.') + 1)
                : fqcn;

        return new AddonTypeBinding() {

            @Override
            public @NotNull String id() {
                return typeId;
            }

            @Override
            public @NotNull TypeBindingMeta meta() {
                return new TypeBindingMeta(
                        "Resolves a single token to a " + fqcn + " constant.",
                        fqcn,
                        List.of(),
                        "1.0.0",
                        false);
            }

            @Override
            public int consumeCount(@NotNull List<String> tokens,
                                    @NotNull EnvironmentAccess env) {
                if (tokens.isEmpty()) return 0;
                String normalized = normalize(tokens.get(0));
                if (frozen.contains(normalized)) return 1;
                if (env.lookupVar(tokens.get(0)) != null) return 1;
                throw new ParseFailureException(
                        "Unknown " + typeId + " value: " + tokens.get(0));
            }

            @Override
            public @NotNull Object parse(@NotNull List<String> tokens,
                                         @NotNull EnvironmentAccess env) {
                String normalized = normalize(tokens.get(0));
                if (frozen.contains(normalized)) return normalized;
                VarHandle ref = env.lookupVar(tokens.get(0));
                if (ref != null) return ref;
                throw new ParseFailureException(
                        "Unknown " + typeId + " value: " + tokens.get(0));
            }

            @Override
            public @NotNull String toJava(@NotNull Object value,
                                          @NotNull CodegenAccess ctx,
                                          @NotNull EnvironmentAccess env) {
                if (value instanceof VarHandle ref) {
                    ctx.addImport(fqcn);
                    return fqcn + ".valueOf(String.valueOf(" + ref.java()
                            + ").toUpperCase().replace(' ', '_').replace('-', '_'))";
                }
                ctx.addImport(fqcn);
                return simpleName + "." + value;
            }
        };
    }

    /**
     * Creates an {@link AddonTypeBinding} by discovering constant names via reflection.
     *
     * <p>Scans the given class for {@code public static} fields whose declared type matches
     * the class itself, collecting their names as the valid constant set.
     *
     * @param typeId the unique type binding identifier
     * @param clazz  the class to scan for static constant fields
     * @param fqcn   the fully qualified class name used for imports
     * @return a ready to register type binding
     * @throws IllegalArgumentException if no static constant fields are found
     */
    public static @NotNull AddonTypeBinding fromStaticFields(
            @NotNull String typeId,
            @NotNull Class<?> clazz,
            @NotNull String fqcn) {
        Set<String> names = new HashSet<>();
        for (Field field : clazz.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (Modifier.isPublic(mods) && Modifier.isStatic(mods) && clazz.isAssignableFrom(field.getType())) {
                names.add(field.getName());
            }
        }
        if (names.isEmpty()) {
            throw new IllegalArgumentException(
                    "No public static constant fields found on " + fqcn
                            + ". Cannot create a registry type binding.");
        }
        return of(typeId, names, fqcn);
    }

    private static @NotNull String normalize(@NotNull String token) {
        return token.toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }
}
