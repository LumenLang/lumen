package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.codegen.BlockAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.codegen.JavaOutput;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.pipeline.persist.GlobalVars;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registers built-in variable manipulation statements.
 *
 * <p>Each math/set operation has two variants:
 * <ul>
 *   <li>Unscoped: operates on the variable in the current scope (e.g. {@code add 5 to score})</li>
 *   <li>Scoped: operates on a global variable for a specific entity (e.g. {@code add 1 to streak for killer})</li>
 * </ul>
 */
@Registration
@SuppressWarnings("unused")
public final class VariableStatements {

    /**
     * Throws a parse-time error if a local variable that would be captured by a lambda
     * is being mutated. Variables defined inside the lambda body are lambda-local and
     * can be mutated freely. Only variables captured from an outer scope must be
     * effectively final (unless they are class-level global fields).
     */
    private static void rejectLocalMutationInsideLambda(@NotNull BlockAccess block,
                                                        @NotNull EnvironmentAccess env,
                                                        @NotNull String varName) {
        if (block.getEnvFromParents("__lambda_block") == null) return;
        if (env.isGlobalField(varName)) return;
        if (!env.isVarCapturedByLambda(varName)) return;
        throw new RuntimeException(
                "Cannot modify local variable '" + varName + "' inside a schedule block. "
                        + "Local variables must be effectively final inside schedule/delay blocks. "
                        + "Tip: use 'global " + varName + " with default <value>' to make it a class-level field instead.");
    }

    private static void emitAutoSave(@NotNull EnvironmentAccess env, @NotNull String varName,
                                     EnvironmentAccess.@NotNull VarHandle ref, @NotNull JavaOutput out) {
        if (env.isStored(varName)) {
            out.line(env.storedClassName(varName) + ".set(" + env.getStoredKey(varName) + ", " + ref.java() + ");");
        }
    }

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
        String scopeKeyPart = ((ObjectType) refType).keyExpression(scopeRef.java());
        return "\"" + info.className() + "." + varName + ".\" + " + scopeKeyPart;
    }

    private static @NotNull String resolveStorageClass(@NotNull EnvironmentAccess.GlobalInfo info) {
        return info.stored() ? "PersistentVars" : "GlobalVars";
    }

    private static void emitScopedMath(@NotNull BindingAccess ctx, @NotNull JavaOutput out,
                                       @NotNull String varName, @NotNull String scopeVarName,
                                       @NotNull String operand, @NotNull String op) {
        EnvironmentAccess env = ctx.env();
        EnvironmentAccess.GlobalInfo info = env.getGlobalInfo(varName);
        if (info == null) {
            throw new RuntimeException("Variable '" + varName
                    + "' is not a global variable. Scoped operations (for ...) are only supported on global vars.");
        }
        if (!info.scoped()) {
            throw new RuntimeException("Variable '" + varName
                    + "' is not a scoped global. Declare it with 'global scoped " + varName + "' to use per-entity access.");
        }
        String storageClass = resolveStorageClass(info);
        String keyExpr = buildScopedKey(env, varName, scopeVarName, info);
        out.line("{");
        out.line("    int __sv = ((Number) " + storageClass + ".get(" + keyExpr + ", " + info.defaultJava() + ")).intValue();");
        out.line("    __sv " + op + " " + operand + ";");
        out.line("    " + storageClass + ".set(" + keyExpr + ", __sv);");
        out.line("}");
    }

    private static void emitScopedDelete(@NotNull BindingAccess ctx, @NotNull JavaOutput out,
                                         @NotNull String varName, @Nullable String scopeVarName) {
        EnvironmentAccess env = ctx.env();
        EnvironmentAccess.GlobalInfo info = env.getGlobalInfo(varName);
        if (info == null) {
            throw new RuntimeException("Variable '" + varName
                    + "' is not a global variable. Scoped operations (for ...) are only supported on global vars.");
        }
        if (!info.scoped()) {
            throw new RuntimeException("Variable '" + varName
                    + "' is not a scoped global. Declare it with 'global scoped " + varName + "' to use per-entity access.");
        }
        String storageClass = resolveStorageClass(info);
        if (scopeVarName != null) {
            String keyExpr = buildScopedKey(env, varName, scopeVarName, info);
            out.line(storageClass + ".delete(" + keyExpr + ");");
        } else {
            String baseKey = "\"" + info.className() + "." + varName + ".\"";
            out.line(storageClass + ".deleteByPrefix(" + baseKey + ");");
        }
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("add %n:INT% to %name:EXPR% for %scope:EXPR%")
                .description("Adds an integer value to a scoped global variable for a specific scope reference.")
                .example("add 1 to streak for killer")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((line, ctx, out) -> emitScopedMath(ctx, out, ctx.java("name"), ctx.java("scope"),
                        ctx.java("n"), "+=")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("add %n:INT% to %name:EXPR%")
                .description("Adds an integer value to a numeric variable.")
                .example("add 5 to score")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((line, ctx, out) -> {
                    EnvironmentAccess env = ctx.env();
                    String varName = ctx.java("name");
                    rejectLocalMutationInsideLambda(ctx.block(), env, varName);
                    EnvironmentAccess.VarHandle ref = env.lookupVar(varName);
                    if (ref == null)
                        throw new RuntimeException("Variable not found: " + varName);
                    out.line(ref.java() + " += " + ctx.java("n") + ";");
                    emitAutoSave(env, varName, ref, out);
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("subtract %n:INT% from %name:EXPR% for %scope:EXPR%")
                .description("Subtracts an integer value from a scoped global variable for a specific scope reference.")
                .example("subtract 1 from streak for player")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((line, ctx, out) -> emitScopedMath(ctx, out, ctx.java("name"), ctx.java("scope"),
                        ctx.java("n"), "-=")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("subtract %n:INT% from %name:EXPR%")
                .description("Subtracts an integer value from a numeric variable.")
                .example("subtract 3 from score")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((line, ctx, out) -> {
                    EnvironmentAccess env = ctx.env();
                    String varName = ctx.java("name");
                    rejectLocalMutationInsideLambda(ctx.block(), env, varName);
                    EnvironmentAccess.VarHandle ref = env.lookupVar(varName);
                    if (ref == null)
                        throw new RuntimeException("Variable not found: " + varName);
                    out.line(ref.java() + " -= " + ctx.java("n") + ";");
                    emitAutoSave(env, varName, ref, out);
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("multiply %name:EXPR% by %n:INT% for %scope:EXPR%")
                .description("Multiplies a scoped global variable by an integer value for a specific scope reference.")
                .example("multiply streak by 2 for killer")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((line, ctx, out) -> emitScopedMath(ctx, out, ctx.java("name"), ctx.java("scope"),
                        ctx.java("n"), "*=")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("multiply %name:EXPR% by %n:INT%")
                .description("Multiplies a numeric variable by an integer value.")
                .example("multiply score by 2")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((line, ctx, out) -> {
                    EnvironmentAccess env = ctx.env();
                    String varName = ctx.java("name");
                    rejectLocalMutationInsideLambda(ctx.block(), env, varName);
                    EnvironmentAccess.VarHandle ref = env.lookupVar(varName);
                    if (ref == null)
                        throw new RuntimeException("Variable not found: " + varName);
                    out.line(ref.java() + " *= " + ctx.java("n") + ";");
                    emitAutoSave(env, varName, ref, out);
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("divide %name:EXPR% by %n:INT% for %scope:EXPR%")
                .description("Divides a scoped global variable by an integer value for a specific scope reference.")
                .example("divide streak by 2 for killer")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((line, ctx, out) -> emitScopedMath(ctx, out, ctx.java("name"), ctx.java("scope"),
                        ctx.java("n"), "/=")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("divide %name:EXPR% by %n:INT%")
                .description("Divides a numeric variable by an integer value.")
                .example("divide score by 2")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((line, ctx, out) -> {
                    EnvironmentAccess env = ctx.env();
                    String varName = ctx.java("name");
                    rejectLocalMutationInsideLambda(ctx.block(), env, varName);
                    EnvironmentAccess.VarHandle ref = env.lookupVar(varName);
                    if (ref == null)
                        throw new RuntimeException("Variable not found: " + varName);
                    out.line(ref.java() + " /= " + ctx.java("n") + ";");
                    emitAutoSave(env, varName, ref, out);
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("delete stored %name:EXPR% for %scope:EXPR%")
                .description("Deletes a scoped global variable for a specific scope reference.")
                .example("delete stored streak for killer")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((line, ctx, out) -> emitScopedDelete(ctx, out, ctx.java("name"), ctx.java("scope"))));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("delete stored %name:EXPR%")
                .description("Deletes a persistent (stored) variable from storage.")
                .example("delete stored myCounter")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((line, ctx, out) -> {
                    EnvironmentAccess env = ctx.env();
                    String varName = ctx.java("name");
                    if (env.isStored(varName)) {
                        String scopeVar = env.getStoredScopeVar(varName);
                        if (scopeVar != null && env.lookupVar(scopeVar) == null) {
                            String baseKey = env.getStoredBaseKey(varName);
                            if (baseKey != null) {
                                out.line(env.storedClassName(varName) + ".deleteByPrefix(" + baseKey + ");");
                                return;
                            }
                        }
                        out.line(env.storedClassName(varName) + ".delete(" + env.getStoredKey(varName) + ");");
                    } else {
                        EnvironmentAccess.GlobalInfo info = env.getGlobalInfo(varName);
                        if (info != null) {
                            ctx.codegen().addImport(GlobalVars.class.getName());
                            if (info.scoped()) {
                                out.line("GlobalVars.deleteByPrefix(\"" + info.className() + "." + varName + ".\");");
                            } else {
                                out.line("GlobalVars.delete(\"" + info.className() + "." + varName + "\");");
                            }
                        } else {
                            String keyExpr = "\"" + ctx.codegen().className() + "." + varName + "\"";
                            out.line(env.persistClassName() + ".delete(" + keyExpr + ");");
                        }
                    }
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %name:EXPR% (for|of) %scope:EXPR%")
                .description("Gets the value of a scoped global variable for a specific scope reference.")
                .example("set spd to get speed for player")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    String varName = ctx.java("name");
                    EnvironmentAccess env = ctx.env();
                    EnvironmentAccess.GlobalInfo info = env.getGlobalInfo(varName);
                    if (info == null) {
                        throw new RuntimeException("'" + varName + "' is not a global variable.");
                    }
                    if (!info.scoped()) {
                        throw new RuntimeException("'" + varName + "' is not a scoped global. Declare it with 'global scoped " + varName + "' to use per-entity access.");
                    }
                    String storageClass = resolveStorageClass(info);
                    String keyExpr = buildScopedKey(env, varName, ctx.java("scope"), info);
                    return new ExpressionResult(storageClass + ".get(" + keyExpr + ", " + info.defaultJava() + ")", info.exprRefTypeId());
                }));
    }
}
