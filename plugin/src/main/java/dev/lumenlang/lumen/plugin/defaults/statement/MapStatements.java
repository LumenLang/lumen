package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.codegen.JavaOutput;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.RefTypeHandle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

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

    private static void emitScopedPut(@NotNull BindingAccess ctx, @NotNull JavaOutput out,
                                      @NotNull String mapVarName, @NotNull String scopeVarName,
                                      @NotNull String keyJava, @NotNull String valJava) {
        EnvironmentAccess env = ctx.env();
        EnvironmentAccess.GlobalInfo info = env.getGlobalInfo(mapVarName);
        if (info == null) {
            throw new RuntimeException("Variable '" + mapVarName
                    + "' is not a global variable. Scoped operations (for ...) are only supported on global vars.");
        }
        String storageClass = info.stored() ? "PersistentVars" : "GlobalVars";
        String storageKey = buildScopedKey(env, mapVarName, scopeVarName, info);
        String tempVar = "__scoped_" + mapVarName;
        ctx.codegen().addImport(Map.class.getName());
        ctx.codegen().addImport(HashMap.class.getName());
        out.line("var " + tempVar + " = " + storageClass + ".get(" + storageKey + ", " + info.defaultJava() + ");");
        out.line("((Map<Object, Object>) " + tempVar + ").put(" + keyJava + ", " + valJava + ");");
        out.line(storageClass + ".set(" + storageKey + ", " + tempVar + ");");
    }

    private static void emitScopedRemove(@NotNull BindingAccess ctx, @NotNull JavaOutput out,
                                         @NotNull String mapVarName, @NotNull String scopeVarName,
                                         @NotNull String keyJava) {
        EnvironmentAccess env = ctx.env();
        EnvironmentAccess.GlobalInfo info = env.getGlobalInfo(mapVarName);
        if (info == null) {
            throw new RuntimeException("Variable '" + mapVarName
                    + "' is not a global variable. Scoped operations (for ...) are only supported on global vars.");
        }
        String storageClass = info.stored() ? "PersistentVars" : "GlobalVars";
        String storageKey = buildScopedKey(env, mapVarName, scopeVarName, info);
        String tempVar = "__scoped_" + mapVarName;
        ctx.codegen().addImport(Map.class.getName());
        ctx.codegen().addImport(HashMap.class.getName());
        out.line("var " + tempVar + " = " + storageClass + ".get(" + storageKey + ", " + info.defaultJava() + ");");
        out.line("((Map<?, ?>) " + tempVar + ").remove(" + keyJava + ");");
        out.line(storageClass + ".set(" + storageKey + ", " + tempVar + ");");
    }

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
        String scopeKeyPart = refType.keyExpression(scopeRef.java());
        return "\"" + info.className() + "." + varName + ".\" + " + scopeKeyPart;
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %map:MAP% at key %key:EXPR% to %val:EXPR% for %scope:EXPR%")
                .description("Sets a key-value pair in a player-scoped stored map. Reads the map from storage, inserts the entry, and writes it back.")
                .example("set balances at key \"money\" to 100 for p")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler((line, ctx, out) -> {
                    String keyJava = ctx.java("key");
                    String valJava = ctx.java("val");
                    String scopeVarName = ctx.java("scope");
                    String mapVarName = ctx.tokens("map").get(0);
                    emitScopedPut(ctx, out, mapVarName, scopeVarName, keyJava, valJava);
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %map:MAP% at key %key:EXPR% to %val:EXPR%")
                .description("Sets a key-value pair in a map. If the key already exists, its value is replaced.")
                .example("set myMap at key \"name\" to \"Steve\"")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler((line, ctx, out) -> {
                    EnvironmentAccess env = ctx.env();
                    String mapJava = ctx.java("map");
                    String keyJava = ctx.java("key");
                    String valJava = ctx.java("val");
                    ctx.codegen().addImport(Map.class.getName());
                    out.line("((Map<Object, Object>) " + mapJava + ").put(" + keyJava + ", " + valJava + ");");
                    flushIfStored(env, out, mapJava, mapVarName(ctx));
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("remove key %key:EXPR% from %map:MAP% for %scope:EXPR%")
                .description("Removes a key from a player-scoped stored map.")
                .example("remove key \"money\" from balances for p")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler((line, ctx, out) -> {
                    String keyJava = ctx.java("key");
                    String scopeVarName = ctx.java("scope");
                    String mapVarName = ctx.tokens("map").get(0);
                    emitScopedRemove(ctx, out, mapVarName, scopeVarName, keyJava);
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("remove key %key:EXPR% from %map:MAP%")
                .description("Removes a key and its associated value from a map.")
                .example("remove key \"name\" from myMap")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler((line, ctx, out) -> {
                    EnvironmentAccess env = ctx.env();
                    String mapJava = ctx.java("map");
                    String keyJava = ctx.java("key");
                    ctx.codegen().addImport(Map.class.getName());
                    out.line("((Map<?, ?>) " + mapJava + ").remove(" + keyJava + ");");
                    flushIfStored(env, out, mapJava, mapVarName(ctx));
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("clear %map:MAP%")
                .description("Removes all entries from a map.")
                .example("clear myMap")
                .since("1.0.0")
                .category(Categories.MAP)
                .handler((line, ctx, out) -> {
                    EnvironmentAccess env = ctx.env();
                    String mapJava = ctx.java("map");
                    ctx.codegen().addImport(Map.class.getName());
                    out.line("((Map<?, ?>) " + mapJava + ").clear();");
                    flushIfStored(env, out, mapJava, mapVarName(ctx));
                }));
    }
}
