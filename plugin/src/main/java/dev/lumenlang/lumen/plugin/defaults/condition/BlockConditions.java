package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

/**
 * Registers condition patterns for checking block properties: type, solid/passable,
 * and air/empty status.
 */
@Registration
@SuppressWarnings("unused")
public final class BlockConditions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen").pattern("%b:BLOCK% type (is|is not) %mat:MATERIAL%")
                .description("Checks if a block's type matches or does not match the given material.")
                .examples("if block type is stone:", "if block type is not air:")
                .since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("is not");
                    return "(" + ctx.java("b") + ".getType() " + (negated ? "!= " : "== ") + ctx.java("mat") + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%b:BLOCK% (is|is not) (solid|passable)")
                .description("Checks if a block is or is not solid.")
                .examples("if block is solid:", "if block is not solid:")
                .since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("is not");
                    return (negated ? "!" : "") + ctx.requireVarHandle("b").java() + ".getType().isSolid()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%b:BLOCK% (is|is not) (air|empty)")
                .description("Checks if a block is or is not air (empty).")
                .examples("if block is air:", "if block is not air:")
                .since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> {
                    boolean negated = ctx.choice(0).equals("is not");
                    return (negated ? "!" : "") + ctx.requireVarHandle("b").java() + ".getType().isAir()";
                }));
    }
}
