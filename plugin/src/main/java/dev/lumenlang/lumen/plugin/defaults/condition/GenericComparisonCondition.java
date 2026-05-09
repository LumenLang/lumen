package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.codegen.TypeEnv;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.NullableType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Registers numeric and string comparison conditions on a known variable.
 */
@Registration(order = 300)
@SuppressWarnings("unused")
public final class GenericComparisonCondition {

    private static @NotNull String suggestionFor(@NotNull LumenType actualType, @NotNull String varName, boolean wantedNumeric) {
        boolean nullable = actualType instanceof NullableType;
        LumenType raw = actualType.unwrap();
        if (wantedNumeric) {
            if (PrimitiveType.STRING.equals(raw)) return "compare strings with 'if " + varName + " is \"value\"'";
            if (PrimitiveType.BOOLEAN.equals(raw)) return "use the variable directly: 'if " + varName + "'";
            if (nullable) return "check existence with 'if " + varName + " is set'";
            return "use a numeric variable, or check existence with 'if " + varName + " is set'";
        }
        if (raw.numeric()) return "compare numbers with 'if " + varName + " > value'";
        if (PrimitiveType.BOOLEAN.equals(raw)) return "use the variable directly: 'if " + varName + "'";
        if (nullable) return "check existence with 'if " + varName + " is set'";
        return "compare references with 'if " + varName + " is set' to test existence";
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
                .pattern("%a:VAR% %op:OP% %b:NUMBER%")
                .description("Numeric comparison between a variable and a number.")
                .example("if damage > 5:")
                .since("1.0.0")
                .category(Categories.MATH)
                .handler(ctx -> {
                    LumenType type = ((TypeEnv.VarHandle) ctx.value("a")).type();
                    if (!type.unwrap().numeric()) {
                        rejectOperand(ctx, ctx.java("a"), type, "number", true);
                    }
                    return ctx.java("a") + " " + ctx.java("op") + " " + ctx.java("b");
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%a:VAR% %op:OP_EQ% %b:QSTRING%")
                .description("String equality comparison between a variable and a quoted string.")
                .example("if name is \"alice\":")
                .since("1.0.0")
                .category(Categories.TEXT)
                .handler(ctx -> {
                    LumenType type = ((TypeEnv.VarHandle) ctx.value("a")).type();
                    if (!PrimitiveType.STRING.equals(type.unwrap())) {
                        rejectOperand(ctx, ctx.java("a"), type, "string", false);
                    }
                    String prefix = ctx.java("op").equals("!=") ? "!" : "";
                    return prefix + ctx.java("a") + ".equalsIgnoreCase(" + ctx.java("b") + ")";
                }));
    }
}
