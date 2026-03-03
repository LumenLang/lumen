package net.vansencool.lumen.plugin.defaults.world;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.api.type.RefTypes;
import net.vansencool.lumen.plugin.util.BlockFillHelper;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Registers statement, condition, and expression patterns for Minecraft blocks.
 */
@Registration
@Description("Registers block-related patterns: set type, break, get block, conditions.")
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class BlockPatterns {

    @Call
    public void register(@NotNull LumenAPI api) {
        registerStatements(api);
        registerConditions(api);
        registerExpressions(api);
        registerExplosions(api);
    }

    private void registerStatements(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %b:BLOCK% type [to] %mat:MATERIAL%")
                .description("Sets the type (material) of a block.")
                .example("set block type to stone").since("1.0.0").category(Categories.BLOCK)
                .handler((line, ctx, out) -> out
                        .line(ctx.java("b") + ".setType(" + ctx.java("mat") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("break %b:BLOCK% naturally")
                .description("Breaks a block naturally, dropping its items as if mined by a player.")
                .example("break block naturally").since("1.0.0").category(Categories.BLOCK)
                .handler((line, ctx, out) -> out.line(ctx.java("b") + ".breakNaturally();")));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %b:BLOCK% data [to] %data:EXPR%")
                .description("Sets raw block data on a block from a string representation.")
                .example("set block data to \"facing=north\"").since("1.0.0").category(Categories.BLOCK)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(Bukkit.class.getName());
                    out.line(ctx.java("b") + ".setBlockData(Bukkit.createBlockData("
                            + ctx.java("b") + ".getType(), " + ctx.java("data") + "));");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("fill (from|between) %a:LOCATION% (to|and) %b:LOCATION% with %mat:MATERIAL%")
                .description("Fills all blocks in a cuboid region between two locations with the given material.")
                .example("fill from loc1 to loc2 with stone").since("1.0.0").category(Categories.BLOCK)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(BlockFillHelper.class.getName());
                    out.line("BlockFillHelper.fill(" + ctx.java("a") + ", " + ctx.java("b")
                            + ", " + ctx.java("mat") + ");");
                }));
    }

    private void registerConditions(@NotNull LumenAPI api) {
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

    private void registerExpressions(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] block at %loc:LOCATION%")
                .description("Returns the block at a given location.")
                .example("var b = block at loc").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("loc") + ".getBlock()",
                        RefTypes.BLOCK.id())));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] %b:BLOCK% type")
                .description("Returns the material name of a block's type.")
                .example("var t = block type").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("b") + ".getType().name()",
                        null)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] %b:BLOCK% location")
                .description("Returns the location of a block.")
                .example("var loc = block location").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("b") + ".getLocation()",
                        RefTypes.LOCATION.id())));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] %b:BLOCK% world")
                .description("Returns the world the block is in.")
                .example("var w = block world").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("b") + ".getWorld()",
                        RefTypes.WORLD.id())));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] %b:BLOCK% (x|y|z)")
                .description("Returns the x, y, or z coordinate of a block.")
                .example("var x = block x").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> {
                    String matched = ctx.java("b");
                    String coord = ctx.choice(0);
                    String method = switch (coord) {
                        case "y" -> ".getLocation().getBlockY()";
                        case "z" -> ".getLocation().getBlockZ()";
                        default -> ".getLocation().getBlockX()";
                    };
                    return new ExpressionResult(
                            matched + method,
                            null);
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] %b:BLOCK% light level")
                .description("Returns the light level at a block's location.")
                .example("var light = block light level").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("b") + ".getLightLevel()",
                        null)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] %b:BLOCK% data [string]")
                .description("Returns the block data as a string representation.")
                .example("var data = block data").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("b") + ".getBlockData().getAsString()",
                        null)));
    }

    private void registerExplosions(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("create explosion at %loc:LOCATION%")
                .description("Creates an explosion with default power (4.0) at the given location.")
                .example("create explosion at player's location")
                .since("1.0.0")
                .category(Categories.WORLD)
                .handler((line, ctx, out) ->
                        out.line(ctx.java("loc") + ".getWorld().createExplosion(" + ctx.java("loc") + ", 4.0F);")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("create explosion at %loc:LOCATION% with (power|radius|strength) %power:NUMBER%")
                .description("Creates an explosion with a specified power at the given location.")
                .example("create explosion at player's location with power 5")
                .since("1.0.0")
                .category(Categories.WORLD)
                .handler((line, ctx, out) ->
                        out.line(ctx.java("loc") + ".getWorld().createExplosion(" + ctx.java("loc")
                                + ", (float) " + ctx.java("power") + ");")));
    }
}
