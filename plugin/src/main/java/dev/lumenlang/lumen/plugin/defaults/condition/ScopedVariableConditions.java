package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.CodegenContext;
import dev.lumenlang.lumen.api.codegen.TypeEnv;
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
    private static @NotNull String buildScopedRead(@NotNull TypeEnv env, @NotNull CodegenContext ctx, @NotNull String varName, @NotNull String scopeVarName) {
        TypeEnv.GlobalInfo info = env.getGlobalInfo(varName);
        if (info == null) {
            throw new RuntimeException("Variable '" + varName
                    + "' is not a global variable. Scoped reads (for ...) are only supported on global vars.");
        }
        if (!info.scoped()) {
            throw new RuntimeException("Variable '" + varName
                    + "' is not a scoped global. Declare it inside a 'global:' block with 'scoped to <type> " + varName + ": <type>' for per-entity access.");
        }
        TypeEnv.VarHandle scopeRef = env.lookupVar(scopeVarName);
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
                .pattern("%a:NUMBER% %op:OP% %b:NUMBER% for %scope:VAR%")
                .description("Numeric comparison between a scoped global variable and a number. "
                        + "The 'for' clause specifies which entity's stored value to read.")
                .example("if tp_toggle > 0 for target:")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    String readExpr = buildScopedRead(ctx.env(), ctx.codegen(), ctx.java("a"), ctx.java("scope"));
                    return readExpr + " " + ctx.java("op") + " " + ctx.java("b");
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:STRING% %op:OP_EQ% %b:STRING% for %scope:VAR%")
                .description("String equality comparison between a scoped global variable and a string. "
                        + "The 'for' clause specifies which entity's stored value to read.")
                .example("if tpa_requester is \"none\" for target:")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    String readExpr = buildScopedRead(ctx.env(), ctx.codegen(), ctx.java("a"), ctx.java("scope"));
                    String prefix = ctx.java("op").equals("!=") ? "!" : "";
                    return prefix + readExpr + ".equalsIgnoreCase(" + ctx.java("b") + ")";
                }));
    }
}
