package dev.lumenlang.lumen.pipeline.conditions;

import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.pipeline.codegen.CodegenContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.HandlerContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.conditions.registry.ConditionRegistry;
import dev.lumenlang.lumen.pipeline.conditions.registry.RegisteredConditionMatch;
import dev.lumenlang.lumen.pipeline.language.exceptions.TokenCarryingException;
import dev.lumenlang.lumen.pipeline.language.match.BoundValue;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * A leaf {@link ConditionExpr} that delegates to a single matched condition handler.
 *
 * <p>A {@code ConditionAtom} wraps one {@link RegisteredConditionMatch}: the result of matching
 * a script condition fragment against a registered condition pattern. Its
 * {@link #toJava(TypeEnvImpl, CodegenContextImpl)} implementation simply calls through to the handler.
 *
 * @see ConditionAnd
 * @see ConditionOr
 * @see ConditionRegistry
 */
public final class ConditionAtom implements ConditionExpr {

    private final RegisteredConditionMatch match;

    /**
     * Creates a new {@code ConditionAtom} wrapping the given match.
     *
     * @param match the successful condition match to delegate to
     */
    public ConditionAtom(RegisteredConditionMatch match) {
        this.match = match;
    }

    @Override
    public String toJava(TypeEnvImpl env, CodegenContextImpl ctx) {
        try {
            int line = env.blockContext() != null ? env.blockContext().line() : 0;
            String raw = env.blockContext() != null ? env.blockContext().raw() : "";
            HandlerContextImpl hctx = new HandlerContextImpl(match.match(), env, ctx, null, null, line, raw);
            return match.reg().handler().handle(hctx);
        } catch (DiagnosticException e) {
            throw e;
        } catch (TokenCarryingException e) {
            throw e;
        } catch (RuntimeException e) {
            List<Token> allTokens = new ArrayList<>();
            for (BoundValue bv : match.match().values().values()) {
                allTokens.addAll(bv.tokens());
            }
            throw new TokenCarryingException(e.getMessage(), allTokens);
        }
    }
}
