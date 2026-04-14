package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.plugin.util.LocationUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Registers built-in condition patterns for location and spatial queries.
 */
@Registration
@SuppressWarnings("unused")
public final class LocationConditions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%loc:LOCATION% (is|is not) inside %min:LOCATION% to %max:LOCATION%")
                .description("Checks if a location is or is not inside a bounding box defined by two corner locations.")
                .examples("if player location is inside corner1 to corner2:", "if player location is not inside corner1 to corner2:")
                .since("1.0.0")
                .category(Categories.LOCATION)
                .handler(ctx -> {
                    ctx.codegen().addImport(LocationUtils.class.getName());
                    String loc = ctx.java("loc");
                    String min = ctx.java("min");
                    String max = ctx.java("max");
                    boolean negated = ctx.choice(0).equals("is not");
                    return (negated ? "LocationUtils.notInside" : "LocationUtils.inside") + "(" + loc + ", " + min + ", " + max + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%who:PLAYER% (is|is not) inside %min:LOCATION% to %max:LOCATION%")
                .description("Checks if a player is or is not inside a bounding box defined by two corner locations.")
                .examples("if player is inside corner1 to corner2:", "if player is not inside corner1 to corner2:")
                .since("1.0.0")
                .category(Categories.LOCATION)
                .handler(ctx -> {
                    ctx.codegen().addImport(LocationUtils.class.getName());
                    String loc = ctx.java("who") + ".getLocation()";
                    String min = ctx.java("min");
                    String max = ctx.java("max");
                    boolean negated = ctx.choice(0).equals("is not");
                    return (negated ? "LocationUtils.notInside" : "LocationUtils.inside") + "(" + loc + ", " + min + ", " + max + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:LOCATION% is in same world as %b:LOCATION%")
                .description("Checks if two locations are in the same world.")
                .example("if player location is in same world as target location:")
                .since("1.0.0")
                .category(Categories.LOCATION)
                .handler(ctx -> {
                    ctx.codegen().addImport(LocationUtils.class.getName());
                    String locA = ctx.java("a");
                    String locB = ctx.java("b");
                    return "LocationUtils.sameWorld(" + locA + ", " + locB + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%loc:LOCATION% is near %target:LOCATION% within %dist:NUMBER%")
                .description("Checks if a location is within a specified distance of another location.")
                .example("if player location is near spawn within 10:")
                .since("1.0.0")
                .category(Categories.LOCATION)
                .handler(ctx -> {
                    ctx.codegen().addImport(LocationUtils.class.getName());
                    String loc = ctx.java("loc");
                    String target = ctx.java("target");
                    String dist = ctx.java("dist");
                    return "LocationUtils.near(" + loc + ", " + target + ", " + dist + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%who:PLAYER% is near %target:LOCATION% within %dist:NUMBER%")
                .description("Checks if a player is within a specified distance of a location.")
                .example("if player is near spawn within 10:")
                .since("1.0.0")
                .category(Categories.LOCATION)
                .handler(ctx -> {
                    ctx.codegen().addImport(LocationUtils.class.getName());
                    String who = ctx.java("who");
                    String loc = who + ".getLocation()";
                    String target = ctx.java("target");
                    String dist = ctx.java("dist");
                    return "LocationUtils.near(" + loc + ", " + target + ", " + dist + ")";
                }));
    }
}
