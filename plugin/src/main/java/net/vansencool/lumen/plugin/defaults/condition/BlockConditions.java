package net.vansencool.lumen.plugin.defaults.condition;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.pattern.Categories;
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
                .by("Lumen").pattern("%b:BLOCK% type is %mat:MATERIAL%")
                .description("Checks if a block's type matches the given material.")
                .example("if block type is stone:").since("1.0.0").category(Categories.BLOCK)
                .handler((match, env, ctx) -> "(" + match.java("b", ctx, env) + ".getType() == "
                        + match.java("mat", ctx, env) + ")"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%b:BLOCK% type is not %mat:MATERIAL%")
                .description("Checks if a block's type does not match the given material.")
                .example("if block type is not air:").since("1.0.0").category(Categories.BLOCK)
                .handler((match, env, ctx) -> "(" + match.java("b", ctx, env) + ".getType() != "
                        + match.java("mat", ctx, env) + ")"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%b:BLOCK% is (solid|passable)")
                .description("Checks if a block is solid (not passable).")
                .example("if block is solid:").since("1.0.0").category(Categories.BLOCK)
                .handler((match, env, ctx) -> match.ref("b").java() + ".getType().isSolid()"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%b:BLOCK% is not (solid|passable)")
                .description("Checks if a block is not solid (is passable).")
                .example("if block is not solid:").since("1.0.0").category(Categories.BLOCK)
                .handler((match, env, ctx) -> "!" + match.ref("b").java() + ".getType().isSolid()"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%b:BLOCK% is (air|empty)")
                .description("Checks if a block is air (empty).")
                .example("if block is air:").since("1.0.0").category(Categories.BLOCK)
                .handler((match, env, ctx) -> match.ref("b").java() + ".getType().isAir()"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%b:BLOCK% is not (air|empty)")
                .description("Checks if a block is not air (not empty).")
                .example("if block is not air:").since("1.0.0").category(Categories.BLOCK)
                .handler((match, env, ctx) -> "!" + match.ref("b").java() + ".getType().isAir()"));
    }
}
