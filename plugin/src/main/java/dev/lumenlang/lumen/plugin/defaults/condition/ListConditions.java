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
                .pattern("%list:LIST% (contains|does not contain) %val:EXPR%")
                .description("Checks if a list contains or does not contain a specific value.")
                .examples("if myList contains \"hello\":", "if myList does not contain \"hello\":")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((match, env, ctx) -> {
                    ctx.addImport(List.class.getName());
                    boolean negated = match.choice(0).equals("does not contain");
                    return (negated ? "!" : "") + "((List<?>) " + match.ref("list").java() + ").contains(" + match.java("val", ctx, env) + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%list:LIST% (is|is not) empty")
                .description("Checks if a list is or is not empty.")
                .examples("if myList is empty:", "if myList is not empty:")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler((match, env, ctx) -> {
                    ctx.addImport(List.class.getName());
                    boolean negated = match.choice(0).equals("is not");
                    return (negated ? "!" : "") + "((List<?>) " + match.ref("list").java() + ").isEmpty()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%list:LIST% (is|is not) empty for %scope:EXPR%")
                .description("Checks if a scoped global list is or is not empty for a specific scope reference.")
                .examples("if todos is empty for player:", "if todos is not empty for player:")
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
                    boolean negated = match.choice(0).equals("is not");
                    return (negated ? "!" : "") + "((List<?>) " + (info.stored() ? "PersistentVars" : "GlobalVars") + ".get(" + "\"" + info.className() + "." + listVarName + ".\" + " + ((ObjectType) scopeType).keyExpression(scopeRef.java()) + ", " + info.defaultJava() + ")).isEmpty()";
                }));
    }
}
