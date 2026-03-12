package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.Types;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Registers built-in math expression patterns (min, max, abs, round, floor,
 * ceil, clamp, random).
 */
@Registration
@SuppressWarnings("unused")
public final class MathExpressions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .patterns("random (between|from) %min:INT% (to|and) %max:INT%",
                        "random %min:INT% to %max:INT%")
                .description("Returns a random integer between min (inclusive) and max (inclusive).")
                .examples("var roll = random 1 to 6",
                        "var rx = random between 0 and 100",
                        "var n = random from low to high")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler(ctx -> {
                    ctx.codegen().addImport(ThreadLocalRandom.class.getName());
                    String min = ctx.java("min");
                    String max = ctx.java("max");
                    return new ExpressionResult(
                            "ThreadLocalRandom.current().nextInt("
                                    + min + ", " + max + " + 1)",
                            null, Types.INT);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .patterns("random decimal (between|from) %min:NUMBER% (to|and) %max:NUMBER%",
                        "random decimal %min:NUMBER% to %max:NUMBER%")
                .description("Returns a random decimal (double) between min (inclusive) and max (exclusive).")
                .examples("var luck = random decimal between 0.0 and 1.0",
                        "var dmg = random decimal 1.5 to 10.0")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler(ctx -> {
                    ctx.codegen().addImport(ThreadLocalRandom.class.getName());
                    String min = ctx.java("min");
                    String max = ctx.java("max");
                    return new ExpressionResult(
                            "ThreadLocalRandom.current().nextDouble("
                                    + min + ", " + max + ")",
                            null, Types.DOUBLE);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("chance %pct:NUMBER%")
                .description("Returns true with the given percentage chance (0 to 100). For example, 'chance 25' succeeds roughly 25% of the time.")
                .examples("var lucky = chance 10",
                        "if chance 50: message player \"Heads!\"")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler(ctx -> {
                    ctx.codegen().addImport(ThreadLocalRandom.class.getName());
                    String pct = ctx.java("pct");
                    return new ExpressionResult(
                            "(ThreadLocalRandom.current().nextDouble(100.0) < ((Number)((Object) "
                                    + pct + ")).doubleValue())",
                            null, Types.BOOLEAN);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .patterns("(min|minimum) of %x:NUMBER% and %y:NUMBER%",
                        "(min|minimum) between %x:NUMBER% and %y:NUMBER%")
                .description("Returns the smaller of two values.")
                .examples("var lowest = min of health and maxHealth",
                        "var lowest = minimum between 10 and 20")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler(ctx -> new ExpressionResult(
                        "Math.min(" + ctx.java("x") + ", " + ctx.java("y") + ")",
                        null, Types.DOUBLE)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .patterns("(max|maximum) of %x:NUMBER% and %y:NUMBER%",
                        "(max|maximum) between %x:NUMBER% and %y:NUMBER%")
                .description("Returns the larger of two values.")
                .examples("var highest = max of health and maxHealth",
                        "var highest = maximum between 10 and 20")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler(ctx -> new ExpressionResult(
                        "Math.max(" + ctx.java("x") + ", " + ctx.java("y") + ")",
                        null, Types.DOUBLE)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("abs of %x:NUMBER%")
                .description("Returns the absolute value of a number.")
                .example("var positive = abs of difference")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler(ctx -> new ExpressionResult(
                        "Math.abs(" + ctx.java("x") + ")",
                        null, Types.DOUBLE)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("round %x:NUMBER%")
                .description("Rounds a number to the nearest integer.")
                .example("var rounded = round 3.7")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler(ctx -> new ExpressionResult(
                        "Math.round(((Number) ((Object) " + ctx.java("x") + ")).doubleValue())",
                        null, Types.LONG)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("floor %x:NUMBER%")
                .description("Rounds a number down to the nearest integer.")
                .example("var floored = floor 3.7")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler(ctx -> new ExpressionResult(
                        "(int) Math.floor(((Number) ((Object) " + ctx.java("x") + ")).doubleValue())",
                        null, Types.INT)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("ceil %x:NUMBER%")
                .description("Rounds a number up to the nearest integer.")
                .example("var ceiled = ceil 3.2")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler(ctx -> new ExpressionResult(
                        "(int) Math.ceil(((Number) ((Object) " + ctx.java("x") + ")).doubleValue())",
                        null, Types.INT)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("clamp %x:NUMBER% between %min:NUMBER% and %max:NUMBER%")
                .description("Clamps a value between a minimum and maximum.")
                .example("var clamped = clamp health between 0 and 20")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler(ctx -> new ExpressionResult(
                        "Math.max(" + ctx.java("min") + ", Math.min(" + ctx.java("x")
                                + ", " + ctx.java("max") + "))",
                        null, Types.DOUBLE)));
    }
}
