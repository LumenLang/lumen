package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.codegen.TypeEnv;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.CollectionType;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.api.type.TypeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Registers built-in statement patterns for map manipulation.
 */
@Registration
@SuppressWarnings("unused")
public final class MapStatements {

    private static @Nullable String mapVarName(@NotNull HandlerContext ctx) {
        Object val = ctx.value("map");
        if (val instanceof TypeEnv.VarHandle ref) {
            return ref.java();
        }
        return null;
    }

    private static void emitScopedMutation(@NotNull HandlerContext ctx, @NotNull String mapVarName, @NotNull String scopeVarName, @NotNull Function<String, String> mutation) {
        TypeEnv.GlobalInfo info = ctx.env().getGlobalInfo(mapVarName);
        if (info == null) {
            throw new DiagnosticException(LumenDiagnostic.error("'" + mapVarName + "' is not a global variable")
                    .at(ctx.block().line(), ctx.block().raw())
                    .label("not a global")
                    .help("declare it inside a 'global:' block as '" + mapVarName + ": map of <key> to <value>'")
                    .build());
        }
        if (!info.scoped()) {
            throw new DiagnosticException(LumenDiagnostic.error("'" + mapVarName + "' is not a scoped global")
                    .at(ctx.block().line(), ctx.block().raw())
                    .label("not scoped")
                    .help("declare it inside a 'global:' block with 'scoped to <type> " + mapVarName + ": <type>' for per-entity access")
                    .build());
        }
        String storageClass = info.stored() ? "PersistentVars" : "GlobalVars";
        String scopeKey = buildScopedKey(ctx, mapVarName, scopeVarName, info);
        String tmp = "__scoped_" + mapVarName + "_" + ctx.out().lineNum();
        ctx.codegen().addImport(Map.class.getName());
        ctx.codegen().addImport(HashMap.class.getName());
        ctx.out().line("var " + tmp + " = " + storageClass + ".get(" + scopeKey + ", " + info.defaultJava() + ");");
        ctx.out().line(mutation.apply(tmp));
        ctx.out().line(storageClass + ".set(" + scopeKey + ", " + tmp + ");");
    }

    private static @NotNull String buildScopedKey(@NotNull HandlerContext ctx, @NotNull String varName, @NotNull String scopeVarName, @NotNull TypeEnv.GlobalInfo info) {
        TypeEnv env = ctx.env();
        TypeEnv.VarHandle scopeRef = env.lookupVar(scopeVarName);
        if (scopeRef == null) {
            throw new DiagnosticException(LumenDiagnostic.error("Scope variable '" + scopeVarName + "' not found")
                    .at(ctx.block().line(), ctx.block().raw())
                    .label("undefined variable")
                    .help("make sure the variable is defined before using it")
                    .build());
        }
        LumenType scopeType = scopeRef.type();
        String scopeKeyPart = ((ObjectType) scopeType).keyExpression(scopeRef.java());
        return "\"" + info.className() + "." + varName + ".\" + " + scopeKeyPart;
    }

    private static @NotNull LumenType mapValueType(@NotNull HandlerContext ctx) {
        TypeEnv.VarHandle mapRef = ctx.varHandle("map");
        if (mapRef != null) {
            CollectionType ct = TypeUtils.asCollection(mapRef.type());
            if (ct != null && ct.typeArguments().size() >= 2) return ct.typeArguments().get(1);
        }
        LumenType actual = ctx.resolvedType("val");
        return actual != null ? actual : PrimitiveType.STRING;
    }

    private static void validateMapEntry(@NotNull HandlerContext ctx) {
        TypeEnv.VarHandle mapRef = ctx.varHandle("map");
        if (mapRef == null) return;
        CollectionType ct = TypeUtils.asCollection(mapRef.type());
        if (ct == null || ct.typeArguments().size() < 2) return;
        LumenType expectedKey = ct.typeArguments().get(0);
        LumenType expectedVal = ct.typeArguments().get(1);
        LumenType actualKey = ctx.resolvedType("key");
        if (actualKey != null && !expectedKey.assignableFrom(actualKey)) {
            throw new DiagnosticException(LumenDiagnostic.error("Map key type mismatch")
                    .at(ctx.block().line(), ctx.block().raw())
                    .label("expected '" + expectedKey.displayName() + "', got '" + actualKey.displayName() + "'")
                    .help("this map only accepts '" + expectedKey.displayName() + "' keys")
                    .build());
        }
        LumenType actualVal = ctx.resolvedType("val");
        if (actualVal != null && !expectedVal.assignableFrom(actualVal)) {
            throw new DiagnosticException(LumenDiagnostic.error("Map value type mismatch")
                    .at(ctx.block().line(), ctx.block().raw())
                    .label("expected '" + expectedVal.displayName() + "', got '" + actualVal.displayName() + "'")
                    .help("this map only accepts '" + expectedVal.displayName() + "' values")
                    .build());
        }
    }

    private static void validateMapKey(@NotNull HandlerContext ctx) {
        TypeEnv.VarHandle mapRef = ctx.varHandle("map");
        if (mapRef == null) return;
        CollectionType ct = TypeUtils.asCollection(mapRef.type());
        if (ct == null || ct.typeArguments().isEmpty()) return;
        LumenType expectedKey = ct.typeArguments().get(0);
        LumenType actualKey = ctx.resolvedType("key");
        if (actualKey == null || expectedKey.assignableFrom(actualKey)) return;
        throw new DiagnosticException(LumenDiagnostic.error("Map key type mismatch")
                .at(ctx.block().line(), ctx.block().raw())
                .label("expected '" + expectedKey.displayName() + "', got '" + actualKey.displayName() + "'")
                .help("this map only accepts '" + expectedKey.displayName() + "' keys")
                .build());
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %map:MAP% at key %key:STRING% to %val:EXPR% for %scope:VAR%")
                .description("Sets a key-value pair in a scoped global map for a specific scope reference. Reads the map from storage, inserts the entry, and writes it back.")
                .example("set balances at key \"money\" to 100 for p")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    validateMapEntry(ctx);
                    String val = ctx.java("val", mapValueType(ctx));
                    emitScopedMutation(ctx, ctx.tokens("map").get(0), ctx.java("scope"), tmp -> "((Map) " + tmp + ").put(" + ctx.java("key") + ", " + val + ");");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %map:MAP% at key %key:STRING% to %val:EXPR%")
                .description("Sets a key-value pair in a map. If the key already exists, its value is replaced.")
                .example("set myMap at key \"name\" to \"Steve\"")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    String mapJava = ctx.java("map");
                    validateMapEntry(ctx);
                    ctx.codegen().addImport(Map.class.getName());
                    ctx.out().line("((Map) " + mapJava + ").put(" + ctx.java("key") + ", " + ctx.java("val", mapValueType(ctx)) + ");");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("remove key %key:STRING% from %map:MAP% for %scope:VAR%")
                .description("Removes a key from a scoped global map for a specific scope reference.")
                .example("remove key \"money\" from balances for p")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    validateMapKey(ctx);
                    emitScopedMutation(ctx, ctx.tokens("map").get(0), ctx.java("scope"), tmp -> "((Map<?, ?>) " + tmp + ").remove(" + ctx.java("key") + ");");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("remove key %key:STRING% from %map:MAP%")
                .description("Removes a key and its associated value from a map.")
                .example("remove key \"name\" from myMap")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    String mapJava = ctx.java("map");
                    validateMapKey(ctx);
                    ctx.codegen().addImport(Map.class.getName());
                    ctx.out().line("((Map<?, ?>) " + mapJava + ").remove(" + ctx.java("key") + ");");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("clear %map:MAP% for %scope:VAR%")
                .description("Removes all entries from a scoped global map for a specific scope reference.")
                .example("clear stats for player")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> emitScopedMutation(ctx, ctx.tokens("map").get(0), ctx.java("scope"), tmp -> "((Map<?, ?>) " + tmp + ").clear();")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("clear %map:MAP%")
                .description("Removes all entries from a map.")
                .example("clear myMap")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler(ctx -> {
                    String mapJava = ctx.java("map");
                    ctx.codegen().addImport(Map.class.getName());
                    ctx.out().line("((Map<?, ?>) " + mapJava + ").clear();");
                }));
    }
}
