package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.BuiltinLumenTypes;
import dev.lumenlang.lumen.api.type.CollectionType;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.pipeline.codegen.BindingContext;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.resolve.SuggestionDiagnostics;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.type.TypeAnnotationParser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers built-in expression patterns for map operations.
 */
@Registration
@SuppressWarnings("unused")
public final class MapExpressions {

    private static @NotNull String buildScopedKey(@NotNull EnvironmentAccess.VarHandle scopeRef, @NotNull String varName, @NotNull EnvironmentAccess.GlobalInfo info) {
        return "\"" + info.className() + "." + varName + ".\" + " + ((ObjectType) scopeRef.type()).keyExpression(scopeRef.java());
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        // TODO: Remove in 1.4.0
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("new map")
                .description("Creates a new empty map. WILL ALWAYS THROW AN ERROR, use 'new map of <key-type> to <value-type>' instead.")
                .example("set myMap to new map")
                .since("1.0.0")
                .category(Categories.MAP)
                .deprecated(true)
                .handler(ctx -> {
                    throw new DiagnosticException(LumenDiagnostic.error("E502", "Untyped maps are no longer supported")
                            .at(ctx.block().line(), ctx.block().raw())
                            .label("use 'new map of <key-type> to <value-type>' instead")
                            .help("example: 'set myMap to new map of string to int'")
                            .build());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("new map [of] %keyType:EXPR% to %valueType:EXPR%")
                .description("Creates a new empty typed map. Entries added to this map will be validated against the declared key and value types.")
                .examples("set scores to new map of string to int", "set data to new map string to player")
                .since("1.2.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    ctx.codegen().addImport(HashMap.class.getName());
                    BindingContext bc = (BindingContext) ctx;
                    TypeEnv env = (TypeEnv) ctx.env();
                    List<Token> keyTokens = bc.bound("keyType").tokens();
                    TypeAnnotationParser.ParseResult keyResult = TypeAnnotationParser.parseDetailed(keyTokens, 0, env::lookupDataSchema);
                    if (keyResult instanceof TypeAnnotationParser.ParseResult.Failure f) {
                        throw new DiagnosticException(SuggestionDiagnostics.buildTypeFailure("E501", "Invalid map key type", ctx.block().line(), ctx.block().raw(), keyTokens, f));
                    }
                    List<Token> valueTokens = bc.bound("valueType").tokens();
                    TypeAnnotationParser.ParseResult valueResult = TypeAnnotationParser.parseDetailed(valueTokens, 0, env::lookupDataSchema);
                    if (valueResult instanceof TypeAnnotationParser.ParseResult.Failure f) {
                        throw new DiagnosticException(SuggestionDiagnostics.buildTypeFailure("E501", "Invalid map value type", ctx.block().line(), ctx.block().raw(), valueTokens, f));
                    }
                    LumenType keyType = ((TypeAnnotationParser.ParseResult.Success) keyResult).parser().type();
                    LumenType valueType = ((TypeAnnotationParser.ParseResult.Success) valueResult).parser().type();
                    return new ExpressionResult("new HashMap<>()", BuiltinLumenTypes.mapOf(keyType, valueType));
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
                        throw new DiagnosticException(LumenDiagnostic.error("E500", "'" + mapVarName + "' is not a global variable")
                                .at(ctx.block().line(), ctx.block().raw())
                                .label("scoped map operations require a global variable")
                                .help("scoped expressions (for ...) are only supported on global vars")
                                .build());
                    }
                    if (!info.scoped()) {
                        throw new DiagnosticException(LumenDiagnostic.error("E502", "'" + mapVarName + "' is not a scoped global")
                                .at(ctx.block().line(), ctx.block().raw())
                                .label("the 'for' keyword requires a scoped global variable")
                                .help("declare it with 'global scoped " + mapVarName + "' to use per-entity access")
                                .build());
                    }
                    EnvironmentAccess.VarHandle scopeRef = env.lookupVar(scopeVarName);
                    if (scopeRef == null) {
                        throw new DiagnosticException(LumenDiagnostic.error("E500", "Scope variable '" + scopeVarName + "' not found")
                                .at(ctx.block().line(), ctx.block().raw())
                                .label("'" + scopeVarName + "' is not defined in this scope")
                                .help("the scope variable must be a player or entity reference")
                                .build());
                    }
                    String storageClass = info.stored() ? "PersistentVars" : "GlobalVars";
                    String storageKey = buildScopedKey(scopeRef, mapVarName, info);
                    ctx.codegen().addImport(Map.class.getName());
                    ctx.codegen().addImport(HashMap.class.getName());
                    return new ExpressionResult(
                            "(String) ((Map<?, ?>) " + storageClass + ".get(" + storageKey + ", "
                                    + info.defaultJava() + ")).get(" + keyJava + ")",
                            PrimitiveType.STRING);
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
                    Object mapVal = ctx.value("map");
                    LumenType valueType = PrimitiveType.STRING;
                    if (mapVal instanceof EnvironmentAccess.VarHandle ref && ref.type() instanceof CollectionType ct && ct.typeArguments().size() >= 2) {
                        valueType = ct.typeArguments().get(1);
                    }
                    String castType = (valueType instanceof PrimitiveType pt) ? pt.boxedName() : valueType.javaTypeName();
                    return new ExpressionResult(
                            "(" + castType + ") ((Map<?, ?>) " + ctx.java("map") + ").get(" + ctx.java("key") + ")",
                            valueType);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("size of %map:MAP%")
                .description("Returns the number of entries in a map.")
                .example("set count to size of myMap")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    ctx.codegen().addImport(Map.class.getName());
                    return new ExpressionResult(
                            "((Map<?, ?>) " + ctx.java("map") + ").size()",
                            PrimitiveType.INT);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%map:MAP% size")
                .description("Returns the number of entries in a map (postfix syntax).")
                .example("set count to myMap size")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    ctx.codegen().addImport(Map.class.getName());
                    return new ExpressionResult(
                            "((Map<?, ?>) " + ctx.java("map") + ").size()",
                            PrimitiveType.INT);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("keys of %map:MAP%")
                .description("Returns a list containing all keys of a map.")
                .example("set allKeys to keys of myMap")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    ctx.codegen().addImport(Map.class.getName());
                    ctx.codegen().addImport(ArrayList.class.getName());
                    return new ExpressionResult(
                            "new ArrayList<>(((Map<?, ?>) " + ctx.java("map") + ").keySet())",
                            BuiltinLumenTypes.listOf(PrimitiveType.STRING));
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("values of %map:MAP%")
                .description("Returns a list containing all values of a map.")
                .example("set allValues to values of myMap")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    ctx.codegen().addImport(Map.class.getName());
                    ctx.codegen().addImport(ArrayList.class.getName());
                    Object mapVal = ctx.value("map");
                    LumenType valueType = PrimitiveType.STRING;
                    if (mapVal instanceof EnvironmentAccess.VarHandle ref && ref.type() instanceof CollectionType ct && ct.typeArguments().size() >= 2) {
                        valueType = ct.typeArguments().get(1);
                    }
                    return new ExpressionResult(
                            "new ArrayList<>(((Map<?, ?>) " + ctx.java("map") + ").values())",
                            BuiltinLumenTypes.listOf(valueType));
                }));
    }
}
