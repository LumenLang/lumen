package net.vansencool.lumen.plugin.defaults.condition;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.EnvironmentAccess;
import net.vansencool.lumen.api.pattern.Categories;
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

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("chance %n:INT%")
                .description("Succeeds with a random chance (0 to 100).")
                .example("if chance 50:")
                .since("1.0.0")
                .category(Categories.SERVER)
                .handler((match, env, ctx) -> {
                    ctx.addImport(ThreadLocalRandom.class.getName());
                    return "(ThreadLocalRandom.current().nextInt(100) < "
                            + match.java("n", ctx, env) + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%v:EXPR% is set")
                .description("Checks if a value is not null.")
                .example("if myVar is set:")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((match, env, ctx) -> match.java("v", ctx, env) + " != null"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%v:EXPR% is not set")
                .description("Checks if a value is null.")
                .example("if myVar is not set:")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((match, env, ctx) -> match.java("v", ctx, env) + " == null"));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%val:EXPR% is between %min:EXPR% and %max:EXPR%")
                .description("Checks if a value is between a minimum and maximum (inclusive).")
                .examples("if player's x is between 100 and 200:", "if score is between 0 and 100:")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler((match, env, ctx) -> {
                    String val = match.java("val", ctx, env);
                    String min = match.java("min", ctx, env);
                    String max = match.java("max", ctx, env);
                    validateExprIdentifier(val, env);
                    validateExprIdentifier(min, env);
                    validateExprIdentifier(max, env);
                    return "(" + val + " >= " + min + " && " + val + " <= " + max + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%val:EXPR% is not between %min:EXPR% and %max:EXPR%")
                .description("Checks if a value is outside a minimum and maximum range.")
                .examples("if player's y is not between 60 and 120:")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler((match, env, ctx) -> {
                    String val = match.java("val", ctx, env);
                    String min = match.java("min", ctx, env);
                    String max = match.java("max", ctx, env);
                    validateExprIdentifier(val, env);
                    validateExprIdentifier(min, env);
                    validateExprIdentifier(max, env);
                    return "(" + val + " < " + min + " || " + val + " > " + max + ")";
                }));
    }

    private static void validateExprIdentifier(@NotNull String java,
            @NotNull EnvironmentAccess env) {
        if (java.matches("[a-zA-Z_][a-zA-Z0-9_]*") && env.lookupVar(java) == null) {
            throw new RuntimeException("Variable '" + java + "' does not exist");
        }
    }
}
