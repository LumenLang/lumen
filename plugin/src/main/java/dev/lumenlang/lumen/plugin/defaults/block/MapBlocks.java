package dev.lumenlang.lumen.plugin.defaults.block;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.codegen.JavaOutput;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static dev.lumenlang.lumen.api.pattern.LumaExample.of;
import static dev.lumenlang.lumen.api.pattern.LumaExample.secondly;
import static dev.lumenlang.lumen.api.pattern.LumaExample.top;

/**
 * Registers the map loop block patterns.
 */

@Registration
@SuppressWarnings("unused")
public final class MapBlocks {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("loop %key:EXPR% %val:EXPR% in %map:MAP%")
                .description("Iterates over each entry in a map, binding the key and value to separate variables.")
                .example(of(
                        top("loop k v in myMap:"),
                        secondly("broadcast \"%{k}%: %{v}%\"")))
                .since("1.0.0")
                .category(Categories.MAP)
                .addVar("key", PrimitiveType.STRING)
                    .varDescription("The current map key, named by the user (e.g. 'k' in 'loop k v in myMap'). The type depends on the map being looped over and is accurate at runtime.")
                .addVar("val", PrimitiveType.STRING)
                    .varDescription("The current map value, named by the user (e.g. 'v' in 'loop k v in myMap'). The type depends on the map being looped over and is accurate at runtime.")
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        if (ctx.block().isRoot()) {
                            throw new DiagnosticException(LumenDiagnostic.error("E502", "A 'loop' block cannot be top level")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .label("top level loop not allowed")
                                    .help("place 'loop' inside an event, command, or other block")
                                    .build());
                        }
                        ctx.codegen().addImport(Map.class.getName());
                        String keyName = ctx.java("key");
                        String valName = ctx.java("val");
                        if (ctx.env().lookupVar(keyName) != null) {
                            throw new DiagnosticException(LumenDiagnostic.error("E502", "Loop variable '" + keyName + "' is already defined")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .label("'" + keyName + "' already exists in this scope")
                                    .help("use a different variable name")
                                    .build());
                        }
                        if (ctx.env().lookupVar(valName) != null) {
                            throw new DiagnosticException(LumenDiagnostic.error("E502", "Loop variable '" + valName + "' is already defined")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .label("'" + valName + "' already exists in this scope")
                                    .help("use a different variable name")
                                    .build());
                        }
                        String entryVar = "__entry_" + keyName + "_" + valName;
                        out.line("for (var " + entryVar + " : ((Map<?, ?>) " + ctx.java("map") + ").entrySet()) {");
                        out.line("var " + keyName + " = " + entryVar + ".getKey();");
                        out.line("var " + valName + " = " + entryVar + ".getValue();");
                        ctx.env().defineVar(keyName, null, keyName);
                        ctx.env().defineVar(valName, null, valName);
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
                    }
                }));

        api.patterns().block(b -> b
                .by("Lumen")
                .pattern("loop %key:EXPR% %val:EXPR% in %map:MAP% for %scope:EXPR%")
                .description("Iterates over each entry of a scoped global map, binding the key and value to separate variables.")
                .example("loop k v in stats for player:")
                .since("1.0.0")
                .category(Categories.MAP)
                .addVar("key", PrimitiveType.STRING)
                    .varDescription("The current map key, named by the user (e.g. 'k' in 'loop k v in stats for player'). The type depends on the map being looped over and is accurate at runtime.")
                .addVar("val", PrimitiveType.STRING)
                    .varDescription("The current map value, named by the user (e.g. 'v' in 'loop k v in stats for player'). The type depends on the map being looped over and is accurate at runtime.")
                .handler(new BlockHandler() {
                    @Override
                    public void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        if (ctx.block().isRoot()) {
                            throw new DiagnosticException(LumenDiagnostic.error("E502", "A 'loop' block cannot be top level")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .label("top level loop not allowed")
                                    .help("place 'loop' inside an event, command, or other block")
                                    .build());
                        }
                        EnvironmentAccess env = ctx.env();
                        String keyName = ctx.java("key");
                        String valName = ctx.java("val");
                        if (env.lookupVar(keyName) != null) {
                            throw new DiagnosticException(LumenDiagnostic.error("E502", "Loop variable '" + keyName + "' is already defined")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .label("'" + keyName + "' already exists in this scope")
                                    .help("use a different variable name")
                                    .build());
                        }
                        if (env.lookupVar(valName) != null) {
                            throw new DiagnosticException(LumenDiagnostic.error("E502", "Loop variable '" + valName + "' is already defined")
                                    .at(ctx.block().line(), ctx.block().raw())
                                    .label("'" + valName + "' already exists in this scope")
                                    .help("use a different variable name")
                                    .build());
                        }

                        String mapVarName = ctx.tokens("map").get(0);
                        String scopeVarName = ctx.java("scope");
                        EnvironmentAccess.GlobalInfo info = env.getGlobalInfo(mapVarName);
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

                        ctx.codegen().addImport(Map.class.getName());
                        ctx.codegen().addImport(HashMap.class.getName());
                        String entryVar = "__entry_" + keyName + "_" + valName;
                        out.line("for (var " + entryVar + " : ((Map<?, ?>) " + (info.stored() ? "PersistentVars" : "GlobalVars") + ".get(" + "\"" + info.className() + "." + mapVarName + ".\" + " + ((ObjectType) scopeType).keyExpression(scopeRef.java()) + ", " + info.defaultJava() + ")).entrySet()) {");
                        out.line("var " + keyName + " = " + entryVar + ".getKey();");
                        out.line("var " + valName + " = " + entryVar + ".getValue();");
                        env.defineVar(keyName, null, keyName);
                        env.defineVar(valName, null, valName);
                    }

                    @Override
                    public void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out) {
                        out.line("}");
                    }
                }));
    }
}
