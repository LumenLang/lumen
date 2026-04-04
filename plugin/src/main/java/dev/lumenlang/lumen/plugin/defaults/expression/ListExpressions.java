package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.RefTypeHandle;
import dev.lumenlang.lumen.api.type.Types;
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
                .example("set myList to new list")
                .since("1.0.0")
                .category(Categories.LIST)
                .returnRefTypeId(Types.LIST.id())
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
                .examples("set arenas to new list of arena", "set scores to new list of number")
                .since("1.0.0")
                .category(Categories.LIST)
                .returnRefTypeId(Types.LIST.id())
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
                .example("set count to size of myList")
                .since("1.0.0")
                .category(Categories.LIST)
                .returnJavaType(Types.INT)
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
                .example("set count to myList size")
                .since("1.0.0")
                .category(Categories.LIST)
                .returnJavaType(Types.INT)
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
                .example("set item to get myList at index 0")
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
                .example("set idx to myList index of \"hello\"")
                .since("1.0.0")
                .category(Categories.LIST)
                .returnJavaType(Types.INT)
                .handler(ctx -> {
                    ctx.codegen().addImport(List.class.getName());
                    return new ExpressionResult(
                            "((List<?>) " + ctx.java("list") + ").indexOf(" + ctx.java("val") + ")",
                            null, Types.INT);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("size of %list:LIST% for %scope:EXPR%")
                .description("Returns the number of elements in a scoped global list for a specific scope reference.")
                .example("set count to size of todos for player")
                .since("1.0.0")
                .category(Categories.LIST)
                .returnJavaType(Types.INT)
                .handler(ctx -> {
                    EnvironmentAccess env = ctx.env();
                    String listVarName = ctx.tokens("list").get(0);
                    EnvironmentAccess.GlobalInfo info = env.getGlobalInfo(listVarName);
                    if (info == null) throw new RuntimeException("'" + listVarName + "' is not a global variable.");
                    String scopeVarName = ctx.java("scope");
                    EnvironmentAccess.VarHandle scopeRef = env.lookupVar(scopeVarName);
                    if (scopeRef == null) throw new RuntimeException("Scope variable not found: " + scopeVarName);
                    RefTypeHandle refType = scopeRef.type();
                    if (refType == null) throw new RuntimeException("Scope variable '" + scopeVarName + "' has no ref type.");
                    ctx.codegen().addImport(List.class.getName());
                    ctx.codegen().addImport(ArrayList.class.getName());
                    return new ExpressionResult("((List<?>) " + (info.stored() ? "PersistentVars" : "GlobalVars") + ".get(" + "\"" + info.className() + "." + listVarName + ".\" + " + refType.keyExpression(scopeRef.java()) + ", " + info.defaultJava() + ")).size()", null, Types.INT);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%list:LIST% size for %scope:EXPR%")
                .description("Returns the number of elements in a scoped global list (postfix syntax).")
                .example("set count to todos size for player")
                .since("1.0.0")
                .category(Categories.LIST)
                .returnJavaType(Types.INT)
                .handler(ctx -> {
                    EnvironmentAccess env = ctx.env();
                    String listVarName = ctx.tokens("list").get(0);
                    EnvironmentAccess.GlobalInfo info = env.getGlobalInfo(listVarName);
                    if (info == null) throw new RuntimeException("'" + listVarName + "' is not a global variable.");
                    String scopeVarName = ctx.java("scope");
                    EnvironmentAccess.VarHandle scopeRef = env.lookupVar(scopeVarName);
                    if (scopeRef == null) throw new RuntimeException("Scope variable not found: " + scopeVarName);
                    RefTypeHandle refType = scopeRef.type();
                    if (refType == null) throw new RuntimeException("Scope variable '" + scopeVarName + "' has no ref type.");
                    ctx.codegen().addImport(List.class.getName());
                    ctx.codegen().addImport(ArrayList.class.getName());
                    return new ExpressionResult("((List<?>) " + (info.stored() ? "PersistentVars" : "GlobalVars") + ".get(" + "\"" + info.className() + "." + listVarName + ".\" + " + refType.keyExpression(scopeRef.java()) + ", " + info.defaultJava() + ")).size()", null, Types.INT);
                }));
    }
}
