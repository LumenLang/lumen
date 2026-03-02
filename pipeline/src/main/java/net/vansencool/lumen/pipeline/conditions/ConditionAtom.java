package net.vansencool.lumen.pipeline.conditions;

import net.vansencool.lumen.pipeline.codegen.CodegenContext;
import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.conditions.registry.ConditionRegistry;
import net.vansencool.lumen.pipeline.conditions.registry.RegisteredConditionMatch;
import net.vansencool.lumen.pipeline.language.exceptions.TokenCarryingException;
import net.vansencool.lumen.pipeline.language.match.BoundValue;
import net.vansencool.lumen.pipeline.language.tokenization.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * A leaf {@link ConditionExpr} that delegates to a single matched condition handler.
 *
 * <p>A {@code ConditionAtom} wraps one {@link RegisteredConditionMatch}: the result of matching
 * a script condition fragment against a registered condition pattern. Its
 * {@link #toJava(TypeEnv, CodegenContext)} implementation simply calls through to the handler.
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
    public String toJava(TypeEnv env, CodegenContext ctx) {
        try {
            return match.reg().handler().handle(match.match(), env, ctx);
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
