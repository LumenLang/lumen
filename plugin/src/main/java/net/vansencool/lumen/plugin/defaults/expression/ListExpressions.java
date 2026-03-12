package net.vansencool.lumen.plugin.defaults.expression;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.api.type.Types;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Registers built-in expression patterns for list operations.
 */
@Registration
@SuppressWarnings("unused")
public final class ListExpressions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("new list")
                .description("Creates a new empty list.")
                .example("var myList = new list")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    ctx.codegen().addImport(ArrayList.class.getName());
                    return new ExpressionResult(
                            "new ArrayList<>()",
                            Types.LIST.id(),
                            Map.of());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("new list of %type:EXPR%")
                .description("Creates a new empty typed list. Elements added to this list will be validated against the declared type.")
                .examples("var arenas = new list of arena", "var scores = new list of number")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    ctx.codegen().addImport(ArrayList.class.getName());
                    String elementType = ctx.tokens("type").get(0).toLowerCase();
                    return new ExpressionResult(
                            "new ArrayList<>()",
                            Types.LIST.id(),
                            Map.of("element_type", elementType));
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("size of %list:LIST%")
                .description("Returns the number of elements in a list.")
                .example("var count = size of myList")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    ctx.codegen().addImport(List.class.getName());
                    return new ExpressionResult(
                            "((List<?>) " + ctx.java("list") + ").size()",
                            null, Types.INT);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%list:LIST% size")
                .description("Returns the number of elements in a list (postfix syntax).")
                .example("var count = myList size")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    ctx.codegen().addImport(List.class.getName());
                    return new ExpressionResult(
                            "((List<?>) " + ctx.java("list") + ").size()",
                            null, Types.INT);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %list:LIST% at [index] %i:INT%")
                .description("Returns the element at a specific index in a list.")
                .example("var item = get myList at index 0")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    ctx.codegen().addImport(List.class.getName());
                    return new ExpressionResult(
                            "((List<?>) " + ctx.java("list") + ").get(" + ctx.java("i") + ")",
                            null);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%list:LIST% index of %val:EXPR%")
                .description("Returns the index of the first occurrence of a value in a list.")
                .example("var idx = myList index of \"hello\"")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    ctx.codegen().addImport(List.class.getName());
                    return new ExpressionResult(
                            "((List<?>) " + ctx.java("list") + ").indexOf(" + ctx.java("val") + ")",
                            null, Types.INT);
                }));
    }
}
