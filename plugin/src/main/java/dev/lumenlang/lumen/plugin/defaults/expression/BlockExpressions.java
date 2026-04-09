package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
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
                .example("set b to block at loc").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("loc") + ".getBlock()",
                        MinecraftTypes.BLOCK)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] %b:BLOCK% type")
                .description("Returns the material name of a block's type.")
                .example("set t to block type").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("b") + ".getType().name()",
                        PrimitiveType.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] %b:BLOCK% location")
                .description("Returns the location of a block.")
                .example("set loc to block location").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("b") + ".getLocation()",
                        MinecraftTypes.LOCATION)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] %b:BLOCK% world")
                .description("Returns the world the block is in.")
                .example("set w to block world").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("b") + ".getWorld()",
                        MinecraftTypes.WORLD)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] %b:BLOCK% (x|y|z)")
                .description("Returns the x, y, or z coordinate of a block.")
                .example("set x to block x").since("1.0.0").category(Categories.BLOCK)
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
                            PrimitiveType.INT);
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] %b:BLOCK% light level")
                .description("Returns the light level at a block's location.")
                .example("set light to block light level").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("b") + ".getLightLevel()",
                        PrimitiveType.INT)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("[get] %b:BLOCK% data [string]")
                .description("Returns the block data as a string representation.")
                .example("set data to block data").since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("b") + ".getBlockData().getAsString()",
                        PrimitiveType.STRING)));
    }
}
