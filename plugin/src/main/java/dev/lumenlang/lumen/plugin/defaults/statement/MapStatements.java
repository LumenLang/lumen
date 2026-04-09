package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.codegen.JavaOutput;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.ObjectType;
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

    private static @Nullable String mapVarName(@NotNull BindingAccess ctx) {
        Object val = ctx.value("map");
        if (val instanceof EnvironmentAccess.VarHandle ref) {
            return ref.java();
        }
        return null;
    }

    private static void flushIfStored(
            @NotNull EnvironmentAccess env,
            @NotNull JavaOutput out,
            @NotNull String mapJava,
            @Nullable String varName) {
        if (varName != null && env.isStored(varName)) {
            out.line(env.storedClassName(varName) + ".set(" + env.getStoredKey(varName) + ", "
                    + mapJava + ");");
        }
    }

    private static void emitScopedMutation(@NotNull BindingAccess ctx, @NotNull JavaOutput out, @NotNull String mapVarName, @NotNull String scopeVarName, @NotNull Function<String, String> mutation) {
        EnvironmentAccess.GlobalInfo info = ctx.env().getGlobalInfo(mapVarName);
        if (info == null) {
            throw new DiagnosticException(LumenDiagnostic.error("E500", "'" + mapVarName + "' is not a global variable")
                    .at(ctx.block().line(), ctx.block().raw())
                    .label("not a global")
                    .help("declare with 'global " + mapVarName + " with default new map'")
                    .build());
        }
        if (!info.scoped()) {
            throw new DiagnosticException(LumenDiagnostic.error("E502", "'" + mapVarName + "' is not a scoped global")
                    .at(ctx.block().line(), ctx.block().raw())
                    .label("not scoped")
                    .help("declare with 'global scoped " + mapVarName + "' to use per-entity access")
                    .build());
        }
        String storageClass = info.stored() ? "PersistentVars" : "GlobalVars";
        String scopeKey = buildScopedKey(ctx, mapVarName, scopeVarName, info);
        String tmp = "__scoped_" + mapVarName + "_" + out.lineNum();
        ctx.codegen().addImport(Map.class.getName());
        ctx.codegen().addImport(HashMap.class.getName());
        out.line("var " + tmp + " = " + storageClass + ".get(" + scopeKey + ", " + info.defaultJava() + ");");
        out.line(mutation.apply(tmp));
        out.line(storageClass + ".set(" + scopeKey + ", " + tmp + ");");
    }

    private static @NotNull String buildScopedKey(@NotNull BindingAccess ctx,
                                                  @NotNull String varName,
                                                  @NotNull String scopeVarName,
                                                  @NotNull EnvironmentAccess.GlobalInfo info) {
        EnvironmentAccess env = ctx.env();
        EnvironmentAccess.VarHandle scopeRef = env.lookupVar(scopeVarName);
        if (scopeRef == null) {
            throw new DiagnosticException(LumenDiagnostic.error("E500", "Scope variable '" + scopeVarName + "' not found")
                    .at(ctx.block().line(), ctx.block().raw())
                    .label("undefined variable")
                    .help("make sure the variable is defined before using it")
                    .build());
        }
        LumenType scopeType = scopeRef.type();
        if (scopeType == null) {
            throw new DiagnosticException(LumenDiagnostic.error("E502", "Scope variable '" + scopeVarName + "' has no type")
                    .at(ctx.block().line(), ctx.block().raw())
                    .label("expected a typed variable")
                    .help("use a typed variable like a player or entity as scope")
                    .build());
        }
        String scopeKeyPart = ((ObjectType) scopeType).keyExpression(scopeRef.java());
        return "\"" + info.className() + "." + varName + ".\" + " + scopeKeyPart;
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %map:MAP% at key %key:EXPR% to %val:EXPR% for %scope:EXPR%")
                .description("Sets a key-value pair in a scoped global map for a specific scope reference. Reads the map from storage, inserts the entry, and writes it back.")
                .example("set balances at key \"money\" to 100 for p")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler((line, ctx, out) -> emitScopedMutation(ctx, out, ctx.tokens("map").get(0), ctx.java("scope"), tmp -> "((Map<Object, Object>) " + tmp + ").put(" + ctx.java("key") + ", " + ctx.java("val") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %map:MAP% at key %key:EXPR% to %val:EXPR%")
                .description("Sets a key-value pair in a map. If the key already exists, its value is replaced.")
                .example("set myMap at key \"name\" to \"Steve\"")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler((line, ctx, out) -> {
                    String mapJava = ctx.java("map");
                    ctx.codegen().addImport(Map.class.getName());
                    out.line("((Map<Object, Object>) " + mapJava + ").put(" + ctx.java("key") + ", " + ctx.java("val") + ");");
                    flushIfStored(ctx.env(), out, mapJava, mapVarName(ctx));
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("remove key %key:EXPR% from %map:MAP% for %scope:EXPR%")
                .description("Removes a key from a scoped global map for a specific scope reference.")
                .example("remove key \"money\" from balances for p")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler((line, ctx, out) -> emitScopedMutation(ctx, out, ctx.tokens("map").get(0), ctx.java("scope"), tmp -> "((Map<?, ?>) " + tmp + ").remove(" + ctx.java("key") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("remove key %key:EXPR% from %map:MAP%")
                .description("Removes a key and its associated value from a map.")
                .example("remove key \"name\" from myMap")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler((line, ctx, out) -> {
                    String mapJava = ctx.java("map");
                    ctx.codegen().addImport(Map.class.getName());
                    out.line("((Map<?, ?>) " + mapJava + ").remove(" + ctx.java("key") + ");");
                    flushIfStored(ctx.env(), out, mapJava, mapVarName(ctx));
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("clear %map:MAP% for %scope:EXPR%")
                .description("Removes all entries from a scoped global map for a specific scope reference.")
                .example("clear stats for player")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler((line, ctx, out) -> emitScopedMutation(ctx, out, ctx.tokens("map").get(0), ctx.java("scope"), tmp -> "((Map<?, ?>) " + tmp + ").clear();")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("clear %map:MAP%")
                .description("Removes all entries from a map.")
                .example("clear myMap")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler((line, ctx, out) -> {
                    String mapJava = ctx.java("map");
                    ctx.codegen().addImport(Map.class.getName());
                    out.line("((Map<?, ?>) " + mapJava + ").clear();");
                    flushIfStored(ctx.env(), out, mapJava, mapVarName(ctx));
                }));
    }
}
