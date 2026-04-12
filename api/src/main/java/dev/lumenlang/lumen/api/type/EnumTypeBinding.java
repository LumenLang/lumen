package dev.lumenlang.lumen.api.type;

import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess.VarHandle;
import dev.lumenlang.lumen.api.exceptions.ParseFailureException;
import dev.lumenlang.lumen.api.util.FuzzyMatch;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Factory for creating {@link AddonTypeBinding} instances that resolve a single token
 * to an enum constant of a given Java enum class.
 *
 * <p>Use {@link #of(String, Class, String)} to create a binding, then register it with
 * {@link TypeRegistrar#register(AddonTypeBinding)}.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * api.types().register(EnumTypeBinding.of(
 *     "DYE_COLOR", DyeColor.class, "org.bukkit.DyeColor"));
 * }</pre>
 *
 * @see AddonTypeBinding
 * @see TypeRegistrar
 */
public final class EnumTypeBinding {

    private EnumTypeBinding() {
    }

    /**
     * Creates an {@link AddonTypeBinding} that resolves a single token to an enum constant.
     *
     * <p>The token is normalized to {@code UPPER_CASE} with spaces and hyphens replaced by
     * underscores. The generated Java code uses the short class name (imported via
     * {@link CodegenAccess#addImport(String)}).
     *
     * @param typeId    the unique type binding identifier (e.g. {@code "DYE_COLOR"})
     * @param enumClass the enum class to validate against
     * @param fqcn      the fully qualified class name used for imports (e.g.
     *                  {@code "org.bukkit.DyeColor"})
     * @param <E>       the enum type
     * @return a ready to register type binding
     */
    public static <E extends Enum<E>> @NotNull AddonTypeBinding of(
            @NotNull String typeId,
            @NotNull Class<E> enumClass,
            @NotNull String fqcn) {
        E[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            throw new IllegalArgumentException(
                    "Cannot load enum constants for " + fqcn
                            + ". The class is not accessible as an enum on this server version.");
        }
        Set<String> constantNames = new HashSet<>();
        for (E e : constants) {
            constantNames.add(e.name());
        }
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
                        "Resolves a single token to a " + fqcn + " enum constant.",
                        fqcn,
                        List.of(),
                        "1.0.0",
                        false);
            }

            @Override
            public @NotNull Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
                String normalized = normalize(tokens.get(0));
                if (frozen.contains(normalized)) return normalized;
                VarHandle ref = env.lookupVar(tokens.get(0));
                if (ref != null) return ref;
                throw new ParseFailureException(rejectMessage(typeId, tokens.get(0), frozen));
            }

            @Override
            public @NotNull String toJava(@NotNull Object value, @NotNull CodegenAccess ctx, @NotNull EnvironmentAccess env) {
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

    private static @NotNull String normalize(@NotNull String token) {
        return token.toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
    }

    private static @NotNull String rejectMessage(@NotNull String typeId, @NotNull String token, @NotNull Set<String> known) {
        String closest = FuzzyMatch.closest(token.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_'), known);
        if (closest != null) return "Unknown " + typeId + " value: " + token + ", did you mean '" + closest.toLowerCase(Locale.ROOT) + "'?";
        return "Unknown " + typeId + " value: " + token;
    }
}
