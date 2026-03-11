package net.vansencool.lumen.plugin.defaults.expression;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.EnvironmentAccess;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.api.type.RefTypeHandle;
import net.vansencool.lumen.api.type.RefTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Registers built-in expression patterns for map operations.
 */
@Registration
@SuppressWarnings("unused")
public final class MapExpressions {

    private static @NotNull String buildScopedKey(@NotNull EnvironmentAccess env,
                                                  @NotNull String varName,
                                                  @NotNull String scopeVarName,
                                                  @NotNull EnvironmentAccess.GlobalInfo info) {
        EnvironmentAccess.VarHandle scopeRef = env.lookupVar(scopeVarName);
        if (scopeRef == null) {
            throw new RuntimeException("Scope variable not found: " + scopeVarName);
        }
        RefTypeHandle refType = scopeRef.type();
        if (refType == null) {
            throw new RuntimeException("Scope variable '" + scopeVarName
                    + "' has no ref type. Expected a typed variable like a player or entity.");
        }
        return "\"" + info.className() + "." + varName + ".\" + " + refType.keyExpression(scopeRef.java());
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("new map")
                .description("Creates a new empty map.")
                .example("var myMap = new map")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    ctx.codegen().addImport(HashMap.class.getName());
                    return new ExpressionResult(
                            "new HashMap<>()",
                            RefTypes.MAP.id(),
                            Map.of());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %map:MAP% at key %key:EXPR% for %scope:EXPR%")
                .description("Returns the value associated with a key in a player-scoped stored map.")
                .example("var bal = get balances at key \"money\" for p")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    String keyJava = ctx.java("key");
                    String scopeVarName = ctx.java("scope");
                    String mapVarName = ctx.tokens("map").get(0);
                    EnvironmentAccess env = ctx.env();
                    EnvironmentAccess.GlobalInfo info = env.getGlobalInfo(mapVarName);
                    if (info == null) {
                        throw new RuntimeException("Variable '" + mapVarName
                                + "' is not a global variable. Scoped expressions (for ...) are only supported on global vars.");
                    }
                    String storageClass = info.stored() ? "PersistentVars" : "GlobalVars";
                    String storageKey = buildScopedKey(env, mapVarName, scopeVarName, info);
                    ctx.codegen().addImport(Map.class.getName());
                    ctx.codegen().addImport(HashMap.class.getName());
                    return new ExpressionResult(
                            "((Map<?, ?>) " + storageClass + ".get(" + storageKey + ", "
                                    + info.defaultJava() + ")).get(" + keyJava + ")",
                            null);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %map:MAP% at key %key:EXPR%")
                .description("Returns the value associated with a key in a map, or null if the key is not present.")
                .example("var name = get myMap at key \"name\"")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    ctx.codegen().addImport(Map.class.getName());
                    return new ExpressionResult(
                            "((Map<?, ?>) " + ctx.java("map") + ").get(" + ctx.java("key") + ")",
                            null);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("size of %map:MAP%")
                .description("Returns the number of entries in a map.")
                .example("var count = size of myMap")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    ctx.codegen().addImport(Map.class.getName());
                    return new ExpressionResult(
                            "((Map<?, ?>) " + ctx.java("map") + ").size()",
                            null);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%map:MAP% size")
                .description("Returns the number of entries in a map (postfix syntax).")
                .example("var count = myMap size")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    ctx.codegen().addImport(Map.class.getName());
                    return new ExpressionResult(
                            "((Map<?, ?>) " + ctx.java("map") + ").size()",
                            null);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("keys of %map:MAP%")
                .description("Returns a list containing all keys of a map.")
                .example("var allKeys = keys of myMap")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    ctx.codegen().addImport(Map.class.getName());
                    ctx.codegen().addImport(ArrayList.class.getName());
                    return new ExpressionResult(
                            "new ArrayList<>(((Map<?, ?>) " + ctx.java("map") + ").keySet())",
                            RefTypes.LIST.id(),
                            Map.of());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("values of %map:MAP%")
                .description("Returns a list containing all values of a map.")
                .example("var allValues = values of myMap")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    ctx.codegen().addImport(Map.class.getName());
                    ctx.codegen().addImport(ArrayList.class.getName());
                    return new ExpressionResult(
                            "new ArrayList<>(((Map<?, ?>) " + ctx.java("map") + ").values())",
                            RefTypes.LIST.id(),
                            Map.of());
                }));
    }
}
