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
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%map:MAP% is empty for %scope:EXPR%")
                .description("Checks if a scoped global map has no entries for a specific scope reference.")
                .example("if stats is empty for player:")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler((match, env, ctx) -> {
                    Object mapVal = match.value("map");
                    if (mapVal instanceof EnvironmentAccess.VarHandle) {
                        throw new RuntimeException("Cannot use 'for <scope>' with a local map variable. Use '%map% is empty' instead, or declare the map as 'global scoped'.");
                    }
                    String mapVarName = (String) mapVal;
                    EnvironmentAccess.GlobalInfo info = env.getGlobalInfo(mapVarName);
                    if (info == null) throw new RuntimeException("'" + mapVarName + "' is not a global variable.");
                    if (!info.scoped()) throw new RuntimeException("'" + mapVarName + "' is not a scoped global. Declare it with 'global scoped " + mapVarName + "' to use per-entity access.");
                    String scopeVarName = match.java("scope", ctx, env);
                    EnvironmentAccess.VarHandle scopeRef = env.lookupVar(scopeVarName);
                    if (scopeRef == null) throw new RuntimeException("Scope variable not found: " + scopeVarName);
                    LumenType scopeType = scopeRef.type();
                    if (scopeType == null) throw new RuntimeException("Scope variable '" + scopeVarName + "' has no type.");
                    ctx.addImport(Map.class.getName());
                    ctx.addImport(HashMap.class.getName());
                    return "((Map<?, ?>) " + (info.stored() ? "PersistentVars" : "GlobalVars") + ".get(" + "\"" + info.className() + "." + mapVarName + ".\" + " + ((ObjectType) scopeType).keyExpression(scopeRef.java()) + ", " + info.defaultJava() + ")).isEmpty()";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%map:MAP% is not empty for %scope:EXPR%")
                .description("Checks if a scoped global map has at least one entry for a specific scope reference.")
                .example("if stats is not empty for player:")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler((match, env, ctx) -> {
                    Object mapVal = match.value("map");
                    if (mapVal instanceof EnvironmentAccess.VarHandle) {
                        throw new RuntimeException("Cannot use 'for <scope>' with a local map variable. Use '%map% is not empty' instead, or declare the map as 'global scoped'.");
                    }
                    String mapVarName = (String) mapVal;
                    EnvironmentAccess.GlobalInfo info = env.getGlobalInfo(mapVarName);
                    if (info == null) throw new RuntimeException("'" + mapVarName + "' is not a global variable.");
                    if (!info.scoped()) throw new RuntimeException("'" + mapVarName + "' is not a scoped global. Declare it with 'global scoped " + mapVarName + "' to use per-entity access.");
                    String scopeVarName = match.java("scope", ctx, env);
                    EnvironmentAccess.VarHandle scopeRef = env.lookupVar(scopeVarName);
                    if (scopeRef == null) throw new RuntimeException("Scope variable not found: " + scopeVarName);
                    LumenType scopeType = scopeRef.type();
                    if (scopeType == null) throw new RuntimeException("Scope variable '" + scopeVarName + "' has no type.");
                    ctx.addImport(Map.class.getName());
                    ctx.addImport(HashMap.class.getName());
                    return "!((Map<?, ?>) " + (info.stored() ? "PersistentVars" : "GlobalVars") + ".get(" + "\"" + info.className() + "." + mapVarName + ".\" + " + ((ObjectType) scopeType).keyExpression(scopeRef.java()) + ", " + info.defaultJava() + ")).isEmpty()";
                }));
    }
}
