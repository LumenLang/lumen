package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import org.jetbrains.annotations.NotNull;

/**
 * Registers location-related expression patterns.
 *
 * <p>Provides expressions for extracting coordinate components from
 * locations, computing distances, and getting the block at a location.
 */
@Registration
@SuppressWarnings("unused")
public final class LocationExpressions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("[get] %loc:LOCATION% x")
                .description("Returns the X coordinate of a location as a double.")
                .example("set px to player location x")
                .since("1.0.0")
                .category(Categories.LOCATION)
                .handler(ctx -> new ExpressionResult(ctx.java("loc") + ".getX()", PrimitiveType.DOUBLE)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("[get] %loc:LOCATION% y")
                .description("Returns the Y coordinate of a location as a double.")
                .example("set py to player location y")
                .since("1.0.0")
                .category(Categories.LOCATION)
                .handler(ctx -> new ExpressionResult(ctx.java("loc") + ".getY()", PrimitiveType.DOUBLE)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("[get] %loc:LOCATION% z")
                .description("Returns the Z coordinate of a location as a double.")
                .example("set pz to player location z")
                .since("1.0.0")
                .category(Categories.LOCATION)
                .handler(ctx -> new ExpressionResult(ctx.java("loc") + ".getZ()", PrimitiveType.DOUBLE)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("distance between %a:LOCATION% and %b:LOCATION%")
                .description("Returns the distance between two locations as a double.")
                .example("set dist to distance between player location and targetLoc")
                .since("1.0.0")
                .category(Categories.LOCATION)
                .handler(ctx -> new ExpressionResult(ctx.java("a") + ".distance(" + ctx.java("b") + ")", PrimitiveType.DOUBLE)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("[get] block at %loc:LOCATION%")
                .description("Returns the block at a given location.")
                .example("set b to block at player location")
                .since("1.0.0")
                .category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(ctx.java("loc") + ".getBlock()", MinecraftTypes.BLOCK)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("[get] %loc:LOCATION% world")
                .description("Returns the world of a location.")
                .example("set w to myLocation world")
                .since("1.0.0")
                .category(Categories.LOCATION)
                .handler(ctx -> new ExpressionResult(ctx.java("loc") + ".getWorld()", MinecraftTypes.WORLD)));
    }
}
