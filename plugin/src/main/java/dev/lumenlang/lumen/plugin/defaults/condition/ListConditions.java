package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.ObjectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers built-in condition patterns for list inspection.
 */
@Registration
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
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%list:LIST% is empty for %scope:EXPR%")
                .description("Checks if a scoped global list has no elements for a specific scope reference.")
                .example("if todos is empty for player:")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((match, env, ctx) -> {
                    Object listVal = match.value("list");
                    if (listVal instanceof EnvironmentAccess.VarHandle) {
                        throw new RuntimeException("Cannot use 'for <scope>' with a local list variable. Use '%list% is empty' instead, or declare the list as 'global scoped'.");
                    }
                    String listVarName = (String) listVal;
                    EnvironmentAccess.GlobalInfo info = env.getGlobalInfo(listVarName);
                    if (info == null) throw new RuntimeException("'" + listVarName + "' is not a global variable.");
                    if (!info.scoped()) throw new RuntimeException("'" + listVarName + "' is not a scoped global. Declare it with 'global scoped " + listVarName + "' to use per-entity access.");
                    String scopeVarName = match.java("scope", ctx, env);
                    EnvironmentAccess.VarHandle scopeRef = env.lookupVar(scopeVarName);
                    if (scopeRef == null) throw new RuntimeException("Scope variable not found: " + scopeVarName);
                    LumenType scopeType = scopeRef.type();
                    ctx.addImport(List.class.getName());
                    ctx.addImport(ArrayList.class.getName());
                    return "((List<?>) " + (info.stored() ? "PersistentVars" : "GlobalVars") +
                            ".get(" + "\"" + info.className() + "." + listVarName + ".\" + " + ((ObjectType) scopeType).keyExpression(scopeRef.java()) + ", " + info.defaultJava() + ")).isEmpty()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%list:LIST% is not empty for %scope:EXPR%")
                .description("Checks if a scoped global list has at least one element for a specific scope reference.")
                .example("if todos is not empty for player:")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((match, env, ctx) -> {
                    Object listVal = match.value("list");
                    if (listVal instanceof EnvironmentAccess.VarHandle) {
                        throw new RuntimeException("Cannot use 'for <scope>' with a local list variable. Use '%list% is not empty' instead, or declare the list as 'global scoped'.");
                    }
                    String listVarName = (String) listVal;
                    EnvironmentAccess.GlobalInfo info = env.getGlobalInfo(listVarName);
                    if (info == null) throw new RuntimeException("'" + listVarName + "' is not a global variable.");
                    if (!info.scoped()) throw new RuntimeException("'" + listVarName + "' is not a scoped global. Declare it with 'global scoped " + listVarName + "' to use per-entity access.");
                    String scopeVarName = match.java("scope", ctx, env);
                    EnvironmentAccess.VarHandle scopeRef = env.lookupVar(scopeVarName);
                    if (scopeRef == null) throw new RuntimeException("Scope variable not found: " + scopeVarName);
                    LumenType scopeType = scopeRef.type();
                    ctx.addImport(List.class.getName());
                    ctx.addImport(ArrayList.class.getName());
                    return "!((List<?>) " + (info.stored() ? "PersistentVars" : "GlobalVars") +
                            ".get(" + "\"" + info.className() + "." + listVarName + ".\" + " + ((ObjectType) scopeType).keyExpression(scopeRef.java()) + ", " + info.defaultJava() + ")).isEmpty()";
                }));
    }
}
