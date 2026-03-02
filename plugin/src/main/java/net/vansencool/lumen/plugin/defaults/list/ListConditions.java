package net.vansencool.lumen.plugin.defaults.list;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Registers built-in condition patterns for list inspection.
 */
@Registration
@Description("Registers list condition patterns: contains, is empty")
@SuppressWarnings("unused")
public final class ListConditions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%list:LIST% contains %val:EXPR%")
                .description("Checks if a list contains a specific value.")
                .example("if myList contains \"hello\":")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((match, env, ctx) -> {
                    ctx.addImport(List.class.getName());
                    return "((List<?>) " + match.ref("list").java()
                            + ").contains(" + match.java("val", ctx, env) + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%list:LIST% does not contain %val:EXPR%")
                .description("Checks if a list does not contain a specific value.")
                .example("if myList does not contain \"hello\":")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((match, env, ctx) -> {
                    ctx.addImport(List.class.getName());
                    return "!((List<?>) " + match.ref("list").java()
                            + ").contains(" + match.java("val", ctx, env) + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%list:LIST% is empty")
                .description("Checks if a list has no elements.")
                .example("if myList is empty:")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((match, env, ctx) -> {
                    ctx.addImport(List.class.getName());
                    return "((List<?>) " + match.ref("list").java()
                            + ").isEmpty()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%list:LIST% is not empty")
                .description("Checks if a list has at least one element.")
                .example("if myList is not empty:")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((match, env, ctx) -> {
                    ctx.addImport(List.class.getName());
                    return "!((List<?>) " + match.ref("list").java()
                            + ").isEmpty()";
                }));
    }
}
