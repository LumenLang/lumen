package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.pipeline.persist.GlobalVars;
import dev.lumenlang.lumen.pipeline.persist.PersistentVars;
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
        if (!info.scoped()) {
            throw new RuntimeException("Variable '" + varName
                    + "' is not a scoped global. Declare it inside a 'global:' block with 'scoped to <type> " + varName + ": <type>' for per-entity access.");
        }
        EnvironmentAccess.VarHandle scopeRef = env.lookupVar(scopeVarName);
        if (scopeRef == null) {
            throw new RuntimeException("Scope variable not found: " + scopeVarName);
        }
        LumenType scopeType = scopeRef.type();
        String scopeKeyPart = ((ObjectType) scopeType).keyExpression(scopeRef.java());
        String keyExpr = "\"" + info.className() + "." + varName + ".\" + " + scopeKeyPart;
        if (info.stored()) {
            ctx.addImport(PersistentVars.class.getName());
            return "PersistentVars.get(" + keyExpr + ", " + info.defaultJava() + ")";
        } else {
            ctx.addImport(GlobalVars.class.getName());
            return "GlobalVars.get(" + keyExpr + ", " + info.defaultJava() + ")";
        }
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:EXPR% %op:OP% %b:EXPR% for %scope:VAR%")
                .description("Compares a scoped global variable against a value using a comparison operator. "
                        + "The 'for' clause specifies which entity's stored value to read.")
                .example("if tp_toggle == 1 for target:")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    String varName = ctx.java("a");
                    String bVal = ctx.java("b");
                    String scopeVarName = ctx.java("scope");
                    String op = ctx.java("op");
                    String readExpr = buildScopedRead(ctx.env(), ctx.codegen(), varName, scopeVarName);
                    if (op.equals("<") || op.equals(">") || op.equals("<=") || op.equals(">=")) {
                        return "((double) " + readExpr + ") " + op + " ((double) " + bVal + ")";
                    }
                    return readExpr + " " + op + " " + bVal;
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:EXPR% (is|equals) %b:QSTRING% for %scope:VAR%")
                .description("Checks if a scoped global variable equals a string value (case-insensitive). "
                        + "The 'for' clause specifies which entity's stored value to read.")
                .example("if tpa_requester is \"none\" for target:")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    String varName = ctx.java("a");
                    String bVal = ctx.java("b");
                    String scopeVarName = ctx.java("scope");
                    String readExpr = buildScopedRead(ctx.env(), ctx.codegen(), varName, scopeVarName);
                    return "String.valueOf(" + readExpr + ").equalsIgnoreCase(String.valueOf(" + bVal + "))";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:EXPR% (is not|does not equal) %b:QSTRING% for %scope:VAR%")
                .description("Checks if a scoped global variable does not equal a string value (case-insensitive). "
                        + "The 'for' clause specifies which entity's stored value to read.")
                .example("if tpa_requester is not \"none\" for target:")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    String varName = ctx.java("a");
                    String bVal = ctx.java("b");
                    String scopeVarName = ctx.java("scope");
                    String readExpr = buildScopedRead(ctx.env(), ctx.codegen(), varName, scopeVarName);
                    return "!String.valueOf(" + readExpr + ").equalsIgnoreCase(String.valueOf(" + bVal + "))";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:EXPR% is set for %scope:VAR%")
                .description("Checks if a scoped global variable is not null for the given entity.")
                .example("if tpa_requester is set for target:")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    String varName = ctx.java("a");
                    String scopeVarName = ctx.java("scope");
                    String readExpr = buildScopedRead(ctx.env(), ctx.codegen(), varName, scopeVarName);
                    return readExpr + " != null";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:EXPR% is not set for %scope:VAR%")
                .description("Checks if a scoped global variable is null for the given entity.")
                .example("if tpa_requester is not set for target:")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    String varName = ctx.java("a");
                    String scopeVarName = ctx.java("scope");
                    String readExpr = buildScopedRead(ctx.env(), ctx.codegen(), varName, scopeVarName);
                    return readExpr + " == null";
                }));
    }
}
