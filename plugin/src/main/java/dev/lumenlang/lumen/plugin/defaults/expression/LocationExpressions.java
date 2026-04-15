package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Registers location-related expression patterns.
 */
@Registration
@SuppressWarnings("unused")
public final class LocationExpressions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("new location in %w:WORLD% at %x:INT% %y:INT% %z:INT%")
                .pattern("new location at %x:INT% %y:INT% %z:INT% in %w:WORLD%")
                .description("Creates a new Location from a world and XYZ coordinates.")
                .example("set loc to new location in myWorld at 100 64 -200")
                .since("1.0.0")
                .category(Categories.LOCATION)
                .handler(ctx -> {
                    ctx.codegen().addImport(Location.class.getName());
                    return new ExpressionResult(
                            "new Location(" + ctx.java("w") + ", " + ctx.java("x") + ", "
                                    + ctx.java("y") + ", " + ctx.java("z") + ")",
                            MinecraftTypes.LOCATION);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("new location in %w:WORLD% at %x:INT% %y:INT% %z:INT% %yaw:NUMBER%")
                .pattern("new location at %x:INT% %y:INT% %z:INT% %yaw:NUMBER% in %w:WORLD%")
                .description("Creates a new Location from a world, XYZ coordinates, and yaw.")
                .example("set loc to new location in myWorld at 100 64 -200 90")
                .since("1.0.0")
                .category(Categories.LOCATION)
                .handler(ctx -> {
                    ctx.codegen().addImport(Location.class.getName());
                    return new ExpressionResult(
                            "new Location(" + ctx.java("w") + ", " + ctx.java("x") + ", " + ctx.java("y") + ", " + ctx.java("z") + ", (float) (" + ctx.java("yaw") + "), 0f)",
                            MinecraftTypes.LOCATION);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("new location in %w:WORLD% at %x:INT% %y:INT% %z:INT% %yaw:NUMBER% %pitch:NUMBER%")
                .pattern("new location at %x:INT% %y:INT% %z:INT% %yaw:NUMBER% %pitch:NUMBER% in %w:WORLD%")
                .description("Creates a new Location from a world, XYZ coordinates, yaw, and pitch.")
                .example("set loc to new location in myWorld at 100 64 -200 90 30")
                .since("1.0.0")
                .category(Categories.LOCATION)
                .handler(ctx -> {
                    ctx.codegen().addImport(Location.class.getName());
                    return new ExpressionResult(
                            "new Location(" + ctx.java("w") + ", " + ctx.java("x") + ", " + ctx.java("y") + ", " + ctx.java("z") + ", (float) (" + ctx.java("yaw") + "), (float) (" + ctx.java("pitch") + "))",
                            MinecraftTypes.LOCATION);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .patterns("get %loc:LOCATION% (x|y|z|yaw|pitch)", "get (x|y|z|yaw|pitch) of %loc:LOCATION%")
                .description("Returns a coordinate or rotation component of a location.")
                .examples("set px to get player location x", "set yaw to get yaw of player location")
                .since("1.0.0")
                .category(Categories.LOCATION)
                .handler(ctx -> {
                    String component = ctx.choice(0);
                    String loc = ctx.java("loc");
                    return switch (component) {
                        case "y" -> new ExpressionResult(loc + ".getY()", PrimitiveType.DOUBLE);
                        case "z" -> new ExpressionResult(loc + ".getZ()", PrimitiveType.DOUBLE);
                        case "yaw" -> new ExpressionResult(loc + ".getYaw()", PrimitiveType.FLOAT);
                        case "pitch" -> new ExpressionResult(loc + ".getPitch()", PrimitiveType.FLOAT);
                        default -> new ExpressionResult(loc + ".getX()", PrimitiveType.DOUBLE);
                    };
                }));

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
                .example("set b to get block at player location")
                .since("1.0.0")
                .category(Categories.BLOCK)
                .handler(ctx -> new ExpressionResult(ctx.java("loc") + ".getBlock()", MinecraftTypes.BLOCK)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %loc:LOCATION% world")
                .description("Returns the world of a location.")
                .example("set w to get player location world")
                .since("1.0.0")
                .category(Categories.LOCATION)
                .handler(ctx -> new ExpressionResult(ctx.java("loc") + ".getWorld()", MinecraftTypes.WORLD)));
    }
}
