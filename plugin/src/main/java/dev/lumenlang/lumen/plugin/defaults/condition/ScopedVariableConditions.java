package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.CodegenContext;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.codegen.TypeEnv;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.api.type.NullableType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.pipeline.persist.GlobalVars;
import dev.lumenlang.lumen.pipeline.persist.PersistentVars;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
    private static @NotNull String buildScopedRead(@NotNull TypeEnv env,
                                                   @NotNull CodegenContext ctx,
                                                   @NotNull String varName,
                                                   @NotNull String scopeVarName) {
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

    private static @NotNull String suggestionFor(@NotNull LumenType actualType, @NotNull String varName, boolean wantedNumeric) {
        boolean nullable = actualType instanceof NullableType;
        LumenType raw = actualType.unwrap();
        if (wantedNumeric) {
            if (PrimitiveType.STRING.equals(raw)) return "compare strings with 'if " + varName + " is \"value\" for <scope>'";
            if (nullable) return "check existence with 'if " + varName + " is set for <scope>'";
            return "use a numeric scoped variable, or check existence with 'if " + varName + " is set for <scope>'";
        }
        if (raw.numeric()) return "compare numbers with 'if " + varName + " > value for <scope>'";
        if (nullable) return "check existence with 'if " + varName + " is set for <scope>'";
        return "compare references with 'if " + varName + " is set for <scope>' to test existence";
    }

    private static void rejectOperand(@NotNull HandlerContext ctx, @NotNull String varName, @NotNull LumenType actualType, @NotNull String wantedKind, boolean wantedNumeric) {
        List<? extends ScriptToken> tokens = ctx.scriptTokens("a");
        LumenDiagnostic.Builder b = LumenDiagnostic.error("'" + varName + "' is a " + actualType.displayName() + ", expected a " + wantedKind)
                .at(ctx.source().currentLine(), ctx.source().currentRaw())
                .help(suggestionFor(actualType, varName, wantedNumeric));
        if (!tokens.isEmpty()) {
            ScriptToken first = tokens.get(0);
            ScriptToken last = tokens.get(tokens.size() - 1);
            b.highlight(first.start(), last.end()).label("'" + actualType.displayName() + "' cannot be compared as a " + wantedKind);
        }
        throw new DiagnosticException(b.build());
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:VAR% %op:OP% %b:NUMBER% for %scope:VAR%")
                .description("Numeric comparison between a scoped global variable and a number. "
                        + "The 'for' clause specifies which entity's stored value to read.")
                .example("if tp_toggle > 0 for target:")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    LumenType type = ((TypeEnv.VarHandle) ctx.value("a")).type();
                    if (!type.unwrap().numeric()) {
                        rejectOperand(ctx, ctx.java("a"), type, "number", true);
                    }
                    String readExpr = buildScopedRead(ctx.env(), ctx.codegen(), ctx.java("a"), ctx.java("scope"));
                    return readExpr + " " + ctx.java("op") + " " + ctx.java("b");
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:VAR% %op:OP_EQ% %b:QSTRING% for %scope:VAR%")
                .description("String equality comparison between a scoped global variable and a quoted string. "
                        + "The 'for' clause specifies which entity's stored value to read.")
                .example("if tpa_requester is \"none\" for target:")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    LumenType type = ((TypeEnv.VarHandle) ctx.value("a")).type();
                    if (!PrimitiveType.STRING.equals(type.unwrap())) {
                        rejectOperand(ctx, ctx.java("a"), type, "string", false);
                    }
                    String readExpr = buildScopedRead(ctx.env(), ctx.codegen(), ctx.java("a"), ctx.java("scope"));
                    String prefix = ctx.java("op").equals("!=") ? "!" : "";
                    return prefix + readExpr + ".equalsIgnoreCase(" + ctx.java("b") + ")";
                }));
    }
}
