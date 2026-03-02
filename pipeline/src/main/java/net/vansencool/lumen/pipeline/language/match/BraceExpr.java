package net.vansencool.lumen.pipeline.language.match;

import net.vansencool.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Marker value stored in a {@link BoundValue} when the matched tokens form a
 * brace-grouped expression ({@code {expr}}).
 *
 * <p>During pattern matching, if a typed placeholder encounters a {@code { ... }} token
 * sequence, the entire brace group is consumed and the inner tokens are stored in this
 * record. The actual expression resolution is deferred to code generation time.
 *
 * @param innerTokens the tokens between the opening and closing braces (exclusive)
 */
public record BraceExpr(@NotNull List<Token> innerTokens) {
}
