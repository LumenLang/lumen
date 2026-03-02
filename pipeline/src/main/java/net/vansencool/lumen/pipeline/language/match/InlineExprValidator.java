package net.vansencool.lumen.pipeline.language.match;

import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Validates whether a candidate InlineExpr token sequence can be resolved
 * as a registered expression pattern.
 *
 * <p>During pattern matching, when a typed binding's fixed consumeCount
 * succeeds but parse fails, the matcher falls back to creating an
 * {@link InlineExpr}. This validator checks that the proposed token
 * sequence actually matches a known expression before accepting it,
 * preventing incorrect boundary splits when multiple typed placeholders
 * appear consecutively without separating literals.
 */
@FunctionalInterface
public interface InlineExprValidator {

    /**
     * Returns {@code true} if the given tokens can be resolved as a valid
     * expression (e.g. via a registered expression pattern).
     *
     * @param tokens the candidate InlineExpr tokens
     * @param env    the current type environment
     * @return true if the token sequence is a valid expression
     */
    boolean canResolve(@NotNull List<Token> tokens, @NotNull TypeEnv env);
}
