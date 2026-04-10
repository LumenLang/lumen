package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Registers generic condition patterns that are not tied to a specific type.
 *
 * <p>
 * These conditions serve as catch-all patterns and should be registered after
 * more specific conditions (like player or offline player conditions) so that
 * specific patterns are tried first.
 */
@Registration(order = 100)
@SuppressWarnings("unused")
public final class GenericConditions {

    private static void validateExprIdentifier(@NotNull String java,
                                               @NotNull EnvironmentAccess env) {
        if (java.matches("[a-zA-Z_][a-zA-Z0-9_]*") && env.lookupVar(java) == null) {
            throw new RuntimeException("Variable '" + java + "' does not exist");
        }
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .patterns("[a] (chance|probability) of %value:INT%", "[a] %value:INT% (chance|probability)")
                .description("Succeeds with a random chance (0 to 100). Values of 100 or more always succeed, values of 0 or less always fail.")
                .example("if a chance of 50%:")
                .since("1.0.0")
                .category(Categories.SERVER)
                .handler((match, env, ctx) -> {
                    ctx.addImport(ThreadLocalRandom.class.getName());
                    return "(ThreadLocalRandom.current().nextInt(100) < " + match.java("value", ctx, env) + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%v:EXPR% (is|is not) set")
                .description("Checks if a value is null or not null.")
                .examples("if myVar is set:", "if myVar is not set:")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((match, env, ctx) -> {
                    String java = match.java("v", ctx, env);
                    validateExprIdentifier(java, env);
                    boolean negated = match.choice(0).equals("is not");
                    return java + (negated ? " == null" : " != null");
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%val:EXPR% (is|is not) between %min:EXPR% and %max:EXPR%")
                .description("Checks if a value is between or outside a minimum and maximum range (inclusive).")
                .examples("if player's x is between 100 and 200:", "if player's y is not between 60 and 120:")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler((match, env, ctx) -> {
                    String val = match.java("val", ctx, env);
                    String min = match.java("min", ctx, env);
                    String max = match.java("max", ctx, env);
                    validateExprIdentifier(val, env);
                    validateExprIdentifier(min, env);
                    validateExprIdentifier(max, env);
                    boolean negated = match.choice(0).equals("is not");
                    if (negated) return "(" + val + " < " + min + " || " + val + " > " + max + ")";
                    return "(" + val + " >= " + min + " && " + val + " <= " + max + ")";
                }));
    }
}
