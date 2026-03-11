package net.vansencool.lumen.plugin.defaults.condition;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.CodegenAccess;
import net.vansencool.lumen.api.codegen.EnvironmentAccess;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.api.type.RefTypeHandle;
import net.vansencool.lumen.pipeline.java.compiled.Coerce;
import org.jetbrains.annotations.NotNull;

/**
 * Registers condition patterns that support scoped variable reads using a
 * {@code for <scope>} clause. This allows comparing a global (stored) variable
 * against a value for a specific entity.
 */
@Registration(order = 250)
@SuppressWarnings("unused")
public final class ScopedVariableConditions {

    /**
     * Builds a Java expression that reads a global (stored or in-memory) variable
     * scoped to the given entity variable.
     *
     * @param env          the environment access
     * @param ctx          the codegen access
     * @param varName      the global variable name
     * @param scopeVarName the scope variable name (e.g. "target")
     * @return a Java expression that reads the scoped value
     */
    private static @NotNull String buildScopedRead(@NotNull EnvironmentAccess env,
                                                   @NotNull CodegenAccess ctx,
                                                   @NotNull String varName,
                                                   @NotNull String scopeVarName) {
        EnvironmentAccess.GlobalInfo info = env.getGlobalInfo(varName);
        if (info == null) {
            throw new RuntimeException("Variable '" + varName
                    + "' is not a global variable. Scoped reads (for ...) are only supported on global vars.");
        }
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
        String keyExpr = "\"" + info.className() + "." + varName + ".\" + " + scopeKeyPart;
        if (info.stored()) {
            ctx.addImport("net.vansencool.lumen.pipeline.persist.PersistentVars");
            return "PersistentVars.get(" + keyExpr + ", " + info.defaultJava() + ")";
        } else {
            ctx.addImport("net.vansencool.lumen.pipeline.persist.GlobalVars");
            return "GlobalVars.get(" + keyExpr + ", " + info.defaultJava() + ")";
        }
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:EXPR% %op:OP% %b:EXPR% for %scope:EXPR%")
                .description("Compares a scoped global variable against a value using a comparison operator. "
                        + "The 'for' clause specifies which entity's stored value to read.")
                .example("if tp_toggle == 1 for target:")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((match, env, ctx) -> {
                    String varName = match.java("a", ctx, env);
                    String bVal = match.java("b", ctx, env);
                    String scopeVarName = match.java("scope", ctx, env);
                    String op = match.java("op", ctx, env);
                    String readExpr = buildScopedRead(env, ctx, varName, scopeVarName);
                    if (op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=")) {
                        ctx.addImport(Coerce.class.getName());
                        return "Coerce.toDouble(" + readExpr + ") " + op + " Coerce.toDouble(" + bVal + ")";
                    }
                    return readExpr + " " + op + " " + bVal;
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:EXPR% (is|equals) %b:QSTRING% for %scope:EXPR%")
                .description("Checks if a scoped global variable equals a string value (case-insensitive). "
                        + "The 'for' clause specifies which entity's stored value to read.")
                .example("if tpa_requester is \"none\" for target:")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((match, env, ctx) -> {
                    String varName = match.java("a", ctx, env);
                    String bVal = match.java("b", ctx, env);
                    String scopeVarName = match.java("scope", ctx, env);
                    String readExpr = buildScopedRead(env, ctx, varName, scopeVarName);
                    return "String.valueOf(" + readExpr + ").equalsIgnoreCase(String.valueOf(" + bVal + "))";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:EXPR% (is not|does not equal) %b:QSTRING% for %scope:EXPR%")
                .description("Checks if a scoped global variable does not equal a string value (case-insensitive). "
                        + "The 'for' clause specifies which entity's stored value to read.")
                .example("if tpa_requester is not \"none\" for target:")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((match, env, ctx) -> {
                    String varName = match.java("a", ctx, env);
                    String bVal = match.java("b", ctx, env);
                    String scopeVarName = match.java("scope", ctx, env);
                    String readExpr = buildScopedRead(env, ctx, varName, scopeVarName);
                    return "!String.valueOf(" + readExpr + ").equalsIgnoreCase(String.valueOf(" + bVal + "))";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:EXPR% is set for %scope:EXPR%")
                .description("Checks if a scoped global variable is not null for the given entity.")
                .example("if tpa_requester is set for target:")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((match, env, ctx) -> {
                    String varName = match.java("a", ctx, env);
                    String scopeVarName = match.java("scope", ctx, env);
                    String readExpr = buildScopedRead(env, ctx, varName, scopeVarName);
                    return readExpr + " != null";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:EXPR% is not set for %scope:EXPR%")
                .description("Checks if a scoped global variable is null for the given entity.")
                .example("if tpa_requester is not set for target:")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler((match, env, ctx) -> {
                    String varName = match.java("a", ctx, env);
                    String scopeVarName = match.java("scope", ctx, env);
                    String readExpr = buildScopedRead(env, ctx, varName, scopeVarName);
                    return readExpr + " == null";
                }));
    }
}
