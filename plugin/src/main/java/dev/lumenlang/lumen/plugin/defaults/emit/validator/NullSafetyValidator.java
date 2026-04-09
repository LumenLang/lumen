package dev.lumenlang.lumen.plugin.defaults.emit.validator;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.emit.EmitContext;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.emit.StatementValidator;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.NullableType;
import dev.lumenlang.lumen.pipeline.type.TypeChecker;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Validates that nullable variables are not used in a context where they are known to be null.
 *
 * <p>This validator runs before pattern matching and checks every identifier token in the statement.
 * If a token refers to a nullable variable whose current null state is {@link TypeEnv.NullState#NULL},
 * a diagnostic error is thrown.
 */
@Registration(order = -1999)
@SuppressWarnings("unused")
public final class NullSafetyValidator implements StatementValidator {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.emitters().statementValidator(this);
    }

    @Override
    public void validate(@NotNull List<? extends ScriptToken> tokens, @NotNull EmitContext ctx) {
        TypeEnv env = (TypeEnv) ctx.env();
        for (ScriptToken t : tokens) {
            if (t.tokenType() != ScriptToken.TokenType.IDENT) continue;
            VarRef ref = env.lookupVar(t.text());
            if (ref == null) continue;
            LumenType type = ref.resolvedType();
            if (!(type instanceof NullableType)) continue;
            TypeEnv.NullState state = env.nullState(t.text());
            if (state != TypeEnv.NullState.NULL) continue;
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
}
