package dev.lumenlang.lumen.plugin.defaults.emit.validator;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.emit.StatementValidator;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.NullableType;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.type.TypeChecker;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Validates that nullable variables are not used in a context where they are known to be null.
 */
@Registration(order = -1999)
@SuppressWarnings("unused")
public final class NullSafetyValidator implements StatementValidator {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.emitters().statementValidator(this);
    }

    @Override
    public void validate(@NotNull List<? extends ScriptToken> tokens, @NotNull HandlerContext ctx) {
        TypeEnv env = (TypeEnv) ctx.env();
        boolean isSetStatement = isSetStatement(tokens);
        for (ScriptToken t : tokens) {
            if (t.tokenType() != ScriptToken.TokenType.IDENT) continue;
            VarRef ref = env.lookupVar(t.text());
            if (ref == null) continue;
            LumenType type = ref.type();
            if (!(type instanceof NullableType)) continue;
            TypeEnv.NullState state = env.nullState(t.text());
            if (state != TypeEnv.NullState.NULL) continue;
            if (isSetStatement && isAssignmentTarget(tokens, t)) continue;
            TypeEnv.NullableVarInfo info = env.nullableVarInfo(t.text());
            TypeEnv.NullAssignmentInfo nullInfo = env.nullAssignmentInfo(t.text());
            int declLine = info != null ? info.declarationLine() : -1;
            String declRaw = info != null ? info.declarationRaw() : null;
            int nullLine = nullInfo != null ? nullInfo.line() : -1;
            String nullRaw = nullInfo != null ? nullInfo.raw() : null;
            LumenDiagnostic diag = TypeChecker.checkNullSafety(type, t.text(), false, ctx.line(), ctx.raw(), t.start(), t.end(), declLine, declRaw, nullLine, nullRaw);
            if (diag != null) throw new DiagnosticException(diag);
        }
    }

    private static boolean isSetStatement(@NotNull List<? extends ScriptToken> tokens) {
        for (ScriptToken t : tokens) {
            if (t.tokenType() == ScriptToken.TokenType.IDENT && t.text().equalsIgnoreCase("set")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAssignmentTarget(@NotNull List<? extends ScriptToken> tokens, @NotNull ScriptToken target) {
        int targetIdx = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i) == target) {
                targetIdx = i;
                break;
            }
        }
        if (targetIdx < 0) return false;
        for (int i = targetIdx + 1; i < tokens.size(); i++) {
            ScriptToken t = tokens.get(i);
            if (t.tokenType() == ScriptToken.TokenType.IDENT && t.text().equalsIgnoreCase("to")) {
                return true;
            }
        }
        return false;
    }
}
