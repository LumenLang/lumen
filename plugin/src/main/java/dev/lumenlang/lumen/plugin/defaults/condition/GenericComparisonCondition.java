package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

/**
 * Registers the generic operator comparison condition ({@code %a:EXPR% %op:OP% %b:EXPR%}).
 *
 * <p>This is registered at order 300, which is intentionally higher than
 * {@link GenericConditions} (100) and string conditions (200).
 * This ensures that specific conditions like null checks ({@code is set})
 * and string equality ({@code is "value"}) are matched before this catch-all
 * numeric comparison pattern.
 */
@Registration(order = 300)
@SuppressWarnings("unused")
public final class GenericComparisonCondition {

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
                .pattern("%a:EXPR% %op:OP% %b:EXPR%")
                .description("Generic comparison between two values using a comparison operator.")
                .example("if damage > 5:")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler(ctx -> {
                    String aVal = ctx.java("a");
                    String bVal = ctx.java("b");
                    validateExprIdentifier(aVal, ctx.env());
                    validateExprIdentifier(bVal, ctx.env());
                    String op = ctx.java("op");
                    if (op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=")) {
                        return "((double) " + aVal + ") " + op + " ((double) " + bVal + ")";
                    }
                    return aVal + " " + op + " " + bVal;
                }));
    }
}
