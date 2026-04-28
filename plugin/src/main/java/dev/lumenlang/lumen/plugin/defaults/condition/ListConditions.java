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
@SuppressWarnings({"unused", "DataFlowIssue"})
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
                .handler(ctx -> {
                    ctx.codegen().addImport(List.class.getName());
                    boolean negated = ctx.choice(0).equals("does not contain");
                    return (negated ? "!" : "") + "((List<?>) " + ctx.requireVarHandle("list").java() + ").contains(" + ctx.java("val") + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%list:LIST% (is|is not) empty")
                .description("Checks if a list is or is not empty.")
                .examples("if myList is empty:", "if myList is not empty:")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    ctx.codegen().addImport(List.class.getName());
                    boolean negated = ctx.choice(0).equals("is not");
                    return (negated ? "!" : "") + "((List<?>) " + ctx.requireVarHandle("list").java() + ").isEmpty()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%list:LIST% (is|is not) empty for %scope:VAR%")
                .description("Checks if a scoped global list is or is not empty for a specific scope reference.")
                .examples("if todos is empty for player:", "if todos is not empty for player:")
                .since("1.0.0")
                .category(Categories.LIST)
                .handler(ctx -> {
                    Object listVal = ctx.value("list");
                    if (listVal instanceof EnvironmentAccess.VarHandle) {
                        throw new RuntimeException("Cannot use 'for <scope>' with a local list variable. Use '<list> is empty' instead, or declare the list inside a 'global:' block with 'scoped to <type>'.");
                    }
                    String listVarName = (String) listVal;
                    EnvironmentAccess.GlobalInfo info = ctx.env().getGlobalInfo(listVarName);
                    if (info == null) throw new RuntimeException("'" + listVarName + "' is not a global variable.");
                    if (!info.scoped())
                        throw new RuntimeException("'" + listVarName + "' is not a scoped global. Declare it inside a 'global:' block with 'scoped to <type> " + listVarName + ": list of <type>' for per-entity access.");
                    String scopeVarName = ctx.java("scope");
                    EnvironmentAccess.VarHandle scopeRef = ctx.env().lookupVar(scopeVarName);
                    if (scopeRef == null) throw new RuntimeException("Scope variable not found: " + scopeVarName);
                    LumenType scopeType = scopeRef.type();
                    ctx.codegen().addImport(List.class.getName());
                    ctx.codegen().addImport(ArrayList.class.getName());
                    boolean negated = ctx.choice(0).equals("is not");
                    return (negated ? "!" : "") + "((List<?>) " + (info.stored() ? "PersistentVars" : "GlobalVars") + ".get(" + "\"" + info.className() + "." + listVarName + ".\" + " + ((ObjectType) scopeType).keyExpression(scopeRef.java()) + ", " + info.defaultJava() + ")).isEmpty()";
                }));
    }
}
