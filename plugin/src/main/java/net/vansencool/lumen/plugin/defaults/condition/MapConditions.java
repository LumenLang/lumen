package net.vansencool.lumen.plugin.defaults.condition;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Registers built-in condition patterns for map inspection.
 */
@Registration
@SuppressWarnings("unused")
public final class MapConditions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%map:MAP% contains key %key:EXPR%")
                .description("Checks if a map contains a specific key.")
                .example("if myMap contains key \"name\":")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler((match, env, ctx) -> {
                    ctx.addImport(Map.class.getName());
                    return "((Map<?, ?>) " + match.ref("map").java()
                            + ").containsKey(" + match.java("key", ctx, env) + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%map:MAP% does not contain key %key:EXPR%")
                .description("Checks if a map does not contain a specific key.")
                .example("if myMap does not contain key \"name\":")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler((match, env, ctx) -> {
                    ctx.addImport(Map.class.getName());
                    return "!((Map<?, ?>) " + match.ref("map").java()
                            + ").containsKey(" + match.java("key", ctx, env) + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%map:MAP% is empty")
                .description("Checks if a map has no entries.")
                .example("if myMap is empty:")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler((match, env, ctx) -> {
                    ctx.addImport(Map.class.getName());
                    return "((Map<?, ?>) " + match.ref("map").java()
                            + ").isEmpty()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%map:MAP% is not empty")
                .description("Checks if a map has at least one entry.")
                .example("if myMap is not empty:")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler((match, env, ctx) -> {
                    ctx.addImport(Map.class.getName());
                    return "!((Map<?, ?>) " + match.ref("map").java()
                            + ").isEmpty()";
                }));
    }
}
