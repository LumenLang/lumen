package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.api.type.Types;
import dev.lumenlang.lumen.api.type.BuiltinLumenTypes;
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
        LumenType refType = scopeRef.type();
        if (refType == null) {
            throw new RuntimeException("Scope variable '" + scopeVarName
                    + "' has no ref type. Expected a typed variable like a player or entity.");
        }
        return "\"" + info.className() + "." + varName + ".\" + " + ((ObjectType) refType).keyExpression(scopeRef.java());
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("new map")
                .description("Creates a new empty map.")
                .example("set myMap to new map")
                .since("1.0.0")
                .category(Categories.MAP)
                .returnRefTypeId(BuiltinLumenTypes.MAP.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(HashMap.class.getName());
                    return new ExpressionResult(
                            "new HashMap<>()",
                            BuiltinLumenTypes.MAP.id(),
                            Map.of());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %map:MAP% at key %key:EXPR% for %scope:EXPR%")
                .description("Returns the value associated with a key in a scoped global map for a specific scope reference.")
                .example("set bal to get balances at key \"money\" for p")
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
                    if (!info.scoped()) {
                        throw new RuntimeException("'" + mapVarName
                                + "' is not a scoped global. Declare it with 'global scoped " + mapVarName + "' to use per-entity access.");
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
                .example("set name to get myMap at key \"name\"")
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
                .example("set count to size of myMap")
                .since("1.0.0")
                .category(Categories.MAP)
                .returnJavaType(Types.INT)
                .handler(ctx -> {
                    ctx.codegen().addImport(Map.class.getName());
                    return new ExpressionResult(
                            "((Map<?, ?>) " + ctx.java("map") + ").size()",
                            null, Types.INT);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%map:MAP% size")
                .description("Returns the number of entries in a map (postfix syntax).")
                .example("set count to myMap size")
                .since("1.0.0")
                .category(Categories.MAP)
                .returnJavaType(Types.INT)
                .handler(ctx -> {
                    ctx.codegen().addImport(Map.class.getName());
                    return new ExpressionResult(
                            "((Map<?, ?>) " + ctx.java("map") + ").size()",
                            null, Types.INT);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("keys of %map:MAP%")
                .description("Returns a list containing all keys of a map.")
                .example("set allKeys to keys of myMap")
                .since("1.0.0")
                .category(Categories.MAP)
                .returnRefTypeId(BuiltinLumenTypes.LIST.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(Map.class.getName());
                    ctx.codegen().addImport(ArrayList.class.getName());
                    return new ExpressionResult(
                            "new ArrayList<>(((Map<?, ?>) " + ctx.java("map") + ").keySet())",
                            BuiltinLumenTypes.LIST.id(),
                            Map.of());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("values of %map:MAP%")
                .description("Returns a list containing all values of a map.")
                .example("set allValues to values of myMap")
                .since("1.0.0")
                .category(Categories.MAP)
                .returnRefTypeId(BuiltinLumenTypes.LIST.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(Map.class.getName());
                    ctx.codegen().addImport(ArrayList.class.getName());
                    return new ExpressionResult(
                            "new ArrayList<>(((Map<?, ?>) " + ctx.java("map") + ").values())",
                            BuiltinLumenTypes.LIST.id(),
                            Map.of());
                }));
    }
}
