package net.vansencool.lumen.plugin.defaults.expression;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.api.type.Types;
import org.jetbrains.annotations.NotNull;

/**
 * Registers expression patterns for retrieving block properties: block at location,
 * type, location, world, coordinates, light level, and block data.
 */
@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class BlockExpressions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] block at %loc:LOCATION%")
                .description("Returns the block at a given location.")
                .example("var b = block at loc").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("loc") + ".getBlock()",
                        Types.BLOCK.id())));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] %b:BLOCK% type")
                .description("Returns the material name of a block's type.")
                .example("var t = block type").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("b") + ".getType().name()",
                        null, Types.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] %b:BLOCK% location")
                .description("Returns the location of a block.")
                .example("var loc = block location").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("b") + ".getLocation()",
                        Types.LOCATION.id())));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] %b:BLOCK% world")
                .description("Returns the world the block is in.")
                .example("var w = block world").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("b") + ".getWorld()",
                        Types.WORLD.id())));

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
                            null, Types.INT);
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] %b:BLOCK% light level")
                .description("Returns the light level at a block's location.")
                .example("var light = block light level").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("b") + ".getLightLevel()",
                        null, Types.INT)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] %b:BLOCK% data [string]")
                .description("Returns the block data as a string representation.")
                .example("var data = block data").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("b") + ".getBlockData().getAsString()",
                        null, Types.STRING)));
    }
}
