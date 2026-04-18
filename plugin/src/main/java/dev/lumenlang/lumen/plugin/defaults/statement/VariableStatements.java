package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.BlockAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.NullableType;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.persist.GlobalVars;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registers built-in variable manipulation statements.
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
    private static void rejectLocalMutationInsideLambda(@NotNull BlockAccess block, @NotNull EnvironmentAccess env, @NotNull String varName) {
        if (block.getEnvFromParents("__lambda_block") == null) return;
        if (env.isGlobalField(varName)) return;
        if (!env.isVarCapturedByLambda(varName)) return;
        throw new RuntimeException(
                "Cannot modify local variable '" + varName + "' inside a schedule block. "
                        + "Local variables must be effectively final inside schedule/delay blocks. "
                        + "Tip: use 'global " + varName + " with default <value>' to make it a class-level field instead.");
    }

    private static void emitAutoSave(@NotNull HandlerContext ctx, @NotNull String varName, @NotNull EnvironmentAccess.VarHandle ref) {
        EnvironmentAccess env = ctx.env();
        if (env.isStored(varName)) {
            ctx.out().line(env.storedClassName(varName) + ".set(" + env.getStoredKey(varName) + ", " + ref.java() + ");");
        }
    }

    private static @NotNull String buildScopedKey(@NotNull HandlerContext ctx, @NotNull String varName, @NotNull String scopeVarName, @NotNull EnvironmentAccess.GlobalInfo info) {
        EnvironmentAccess env = ctx.env();
        EnvironmentAccess.VarHandle scopeRef = env.lookupVar(scopeVarName);
        if (scopeRef == null) {
            throw new RuntimeException("Scope variable not found: " + scopeVarName);
        }
        LumenType scopeType = scopeRef.type();
        if (scopeType instanceof NullableType nt) {
            TypeEnv tenv = (TypeEnv) env;
            if (tenv.nullState(scopeRef.java()) != TypeEnv.NullState.NON_NULL) {
                tenv.addWarning(LumenDiagnostic.warning("W301", "Nullable scope used for scoped global access")
                        .at(ctx.line(), ctx.raw())
                        .label("'" + scopeVarName + "' is " + scopeType.displayName() + " and may be null at runtime")
                        .help("narrow first with 'if " + scopeVarName + " is set:' to avoid a possible NullPointerException")
                        .build());
            }
            scopeType = nt.inner();
        }
        if (!(scopeType instanceof ObjectType obj)) {
            throw new DiagnosticException(LumenDiagnostic.error("E301", "Invalid scope variable type")
                    .at(ctx.line(), ctx.raw())
                    .label("'" + scopeVarName + "' has type " + scopeType.displayName() + " which cannot be used as a scope")
                    .help("scoped globals require an entity, player, or other object reference")
                    .build());
        }
        String scopeKeyPart = obj.keyExpression(scopeRef.java());
        return "\"" + info.className() + "." + varName + ".\" + " + scopeKeyPart;
    }

    private static @NotNull String resolveStorageClass(@NotNull EnvironmentAccess.GlobalInfo info) {
        return info.stored() ? "PersistentVars" : "GlobalVars";
    }

    private static void emitScopedMath(@NotNull HandlerContext ctx, @NotNull String varName, @NotNull String scopeVarName, @NotNull String operand, @NotNull String op) {
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
        String keyExpr = buildScopedKey(ctx, varName, scopeVarName, info);
        ctx.out().line("{");
        ctx.out().line("    int __sv = ((Number) " + storageClass + ".get(" + keyExpr + ", " + info.defaultJava() + ")).intValue();");
        ctx.out().line("    __sv " + op + " " + operand + ";");
        ctx.out().line("    " + storageClass + ".set(" + keyExpr + ", __sv);");
        ctx.out().line("}");
    }

    private static void emitScopedDelete(@NotNull HandlerContext ctx, @NotNull String varName, @Nullable String scopeVarName) {
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
            String keyExpr = buildScopedKey(ctx, varName, scopeVarName, info);
            ctx.out().line(storageClass + ".delete(" + keyExpr + ");");
        } else {
            String baseKey = "\"" + info.className() + "." + varName + ".\"";
            ctx.out().line(storageClass + ".deleteByPrefix(" + baseKey + ");");
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
                .handler(ctx -> emitScopedMath(ctx, ctx.java("name"), ctx.java("scope"),
                        ctx.java("n"), "+=")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("add %n:INT% to %name:EXPR%")
                .description("Adds an integer value to a numeric variable.")
                .example("add 5 to score")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    EnvironmentAccess env = ctx.env();
                    String varName = ctx.java("name");
                    rejectLocalMutationInsideLambda(ctx.block(), env, varName);
                    EnvironmentAccess.VarHandle ref = env.lookupVar(varName);
                    if (ref == null)
                        throw new RuntimeException("Variable not found: " + varName);
                    ctx.out().line(ref.java() + " += " + ctx.java("n") + ";");
                    emitAutoSave(ctx, varName, ref);
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("subtract %n:INT% from %name:EXPR% for %scope:EXPR%")
                .description("Subtracts an integer value from a scoped global variable for a specific scope reference.")
                .example("subtract 1 from streak for player")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> emitScopedMath(ctx, ctx.java("name"), ctx.java("scope"),
                        ctx.java("n"), "-=")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("subtract %n:INT% from %name:EXPR%")
                .description("Subtracts an integer value from a numeric variable.")
                .example("subtract 3 from score")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    EnvironmentAccess env = ctx.env();
                    String varName = ctx.java("name");
                    rejectLocalMutationInsideLambda(ctx.block(), env, varName);
                    EnvironmentAccess.VarHandle ref = env.lookupVar(varName);
                    if (ref == null)
                        throw new RuntimeException("Variable not found: " + varName);
                    ctx.out().line(ref.java() + " -= " + ctx.java("n") + ";");
                    emitAutoSave(ctx, varName, ref);
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("multiply %name:EXPR% by %n:INT% for %scope:EXPR%")
                .description("Multiplies a scoped global variable by an integer value for a specific scope reference.")
                .example("multiply streak by 2 for killer")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> emitScopedMath(ctx, ctx.java("name"), ctx.java("scope"),
                        ctx.java("n"), "*=")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("multiply %name:EXPR% by %n:INT%")
                .description("Multiplies a numeric variable by an integer value.")
                .example("multiply score by 2")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    EnvironmentAccess env = ctx.env();
                    String varName = ctx.java("name");
                    rejectLocalMutationInsideLambda(ctx.block(), env, varName);
                    EnvironmentAccess.VarHandle ref = env.lookupVar(varName);
                    if (ref == null)
                        throw new RuntimeException("Variable not found: " + varName);
                    ctx.out().line(ref.java() + " *= " + ctx.java("n") + ";");
                    emitAutoSave(ctx, varName, ref);
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("divide %name:EXPR% by %n:INT% for %scope:EXPR%")
                .description("Divides a scoped global variable by an integer value for a specific scope reference.")
                .example("divide streak by 2 for killer")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> emitScopedMath(ctx, ctx.java("name"), ctx.java("scope"),
                        ctx.java("n"), "/=")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("divide %name:EXPR% by %n:INT%")
                .description("Divides a numeric variable by an integer value.")
                .example("divide score by 2")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    EnvironmentAccess env = ctx.env();
                    String varName = ctx.java("name");
                    rejectLocalMutationInsideLambda(ctx.block(), env, varName);
                    EnvironmentAccess.VarHandle ref = env.lookupVar(varName);
                    if (ref == null)
                        throw new RuntimeException("Variable not found: " + varName);
                    ctx.out().line(ref.java() + " /= " + ctx.java("n") + ";");
                    emitAutoSave(ctx, varName, ref);
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("delete stored %name:EXPR% for %scope:EXPR%")
                .description("Deletes a scoped global variable for a specific scope reference.")
                .example("delete stored streak for killer")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> emitScopedDelete(ctx, ctx.java("name"), ctx.java("scope"))));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("delete stored %name:EXPR%")
                .description("Deletes a persistent (stored) variable from storage.")
                .example("delete stored myCounter")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    EnvironmentAccess env = ctx.env();
                    String varName = ctx.java("name");
                    if (env.isStored(varName)) {
                        String scopeVar = env.getStoredScopeVar(varName);
                        if (scopeVar != null && env.lookupVar(scopeVar) == null) {
                            String baseKey = env.getStoredBaseKey(varName);
                            if (baseKey != null) {
                                ctx.out().line(env.storedClassName(varName) + ".deleteByPrefix(" + baseKey + ");");
                                return;
                            }
                        }
                        ctx.out().line(env.storedClassName(varName) + ".delete(" + env.getStoredKey(varName) + ");");
                    } else {
                        EnvironmentAccess.GlobalInfo info = env.getGlobalInfo(varName);
                        if (info != null) {
                            ctx.codegen().addImport(GlobalVars.class.getName());
                            if (info.scoped()) {
                                ctx.out().line("GlobalVars.deleteByPrefix(\"" + info.className() + "." + varName + ".\");");
                            } else {
                                ctx.out().line("GlobalVars.delete(\"" + info.className() + "." + varName + "\");");
                            }
                        } else {
                            String keyExpr = "\"" + ctx.codegen().className() + "." + varName + "\"";
                            ctx.out().line(env.persistClassName() + ".delete(" + keyExpr + ");");
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
                    String keyExpr = buildScopedKey(ctx, varName, ctx.java("scope"), info);
                    return new ExpressionResult(storageClass + ".get(" + keyExpr + ", " + info.defaultJava() + ")", info.type());
                }));
    }
}
