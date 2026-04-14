package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.ObjectType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Registers built-in condition patterns for map inspection.
 */
@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class MapConditions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%map:MAP% (contains|does not contain) key %key:STRING%")
                .description("Checks if a map contains or does not contain a specific key.")
                .examples("if myMap contains key \"name\":", "if myMap does not contain key \"name\":")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    ctx.codegen().addImport(Map.class.getName());
                    boolean negated = ctx.choice(0).equals("does not contain");
                    return (negated ? "!" : "") + "((Map<?, ?>) " + ctx.requireVarHandle("map").java() + ").containsKey(" + ctx.java("key") + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%map:MAP% (is|is not) empty")
                .description("Checks if a map is or is not empty.")
                .examples("if myMap is empty:", "if myMap is not empty:")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    ctx.codegen().addImport(Map.class.getName());
                    boolean negated = ctx.choice(0).equals("is not");
                    return (negated ? "!" : "") + "((Map<?, ?>) " + ctx.requireVarHandle("map").java() + ").isEmpty()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%map:MAP% (is|is not) empty for %scope:EXPR%")
                .description("Checks if a scoped global map is or is not empty for a specific scope reference.")
                .examples("if stats is empty for player:", "if stats is not empty for player:")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    Object mapVal = ctx.value("map");
                    if (mapVal instanceof EnvironmentAccess.VarHandle) {
                        throw new RuntimeException("Cannot use 'for <scope>' with a local map variable. Use '%map% is empty' instead, or declare the map as 'global scoped'.");
                    }
                    String mapVarName = (String) mapVal;
                    EnvironmentAccess.GlobalInfo info = ctx.env().getGlobalInfo(mapVarName);
                    if (info == null) throw new RuntimeException("'" + mapVarName + "' is not a global variable.");
                    if (!info.scoped()) throw new RuntimeException("'" + mapVarName + "' is not a scoped global. Declare it with 'global scoped " + mapVarName + "' to use per-entity access.");
                    String scopeVarName = ctx.java("scope");
                    EnvironmentAccess.VarHandle scopeRef = ctx.env().lookupVar(scopeVarName);
                    if (scopeRef == null) throw new RuntimeException("Scope variable not found: " + scopeVarName);
                    LumenType scopeType = scopeRef.type();
                    ctx.codegen().addImport(Map.class.getName());
                    ctx.codegen().addImport(HashMap.class.getName());
                    boolean negated = ctx.choice(0).equals("is not");
                    return (negated ? "!" : "") + "((Map<?, ?>) " + (info.stored() ? "PersistentVars" : "GlobalVars") + ".get(" + "\"" + info.className() + "." + mapVarName + ".\" + " + ((ObjectType) scopeType).keyExpression(scopeRef.java()) + ", " + info.defaultJava() + ")).isEmpty()";
                }));
    }
}
