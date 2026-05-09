package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

/**
 * Registers numeric and string comparison conditions.
 */
@Registration(order = 300)
@SuppressWarnings("unused")
public final class GenericComparisonCondition {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:NUMBER% %op:OP% %b:NUMBER%")
                .description("Numeric comparison between two numeric values (literal or variable).")
                .example("if damage > 5:")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler(ctx -> ctx.java("a") + " " + ctx.java("op") + " " + ctx.java("b")));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% %op:OP_EQ% %b:STRING%")
                .description("String equality comparison between two strings (literal or variable).")
                .example("if name is \"alice\":")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx -> (ctx.java("op").equals("!=") ? "!" : "") + ctx.java("a") + ".equalsIgnoreCase(" + ctx.java("b") + ")"));
    }
}
