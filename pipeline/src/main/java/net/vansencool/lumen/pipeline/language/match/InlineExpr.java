package net.vansencool.lumen.pipeline.language.match;

import net.vansencool.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Marker value stored in a {@link BoundValue} when the matched tokens form an
 * inline expression that could not be parsed directly by the target type binding.
 *
 * <p>During pattern matching, if a typed placeholder's normal parse fails (e.g. an
 * {@code INT} placeholder encountering multi-token expressions like
 * {@code get player's x}), the matcher falls back to greedy backtracking. For
 * multi-token slices where the binding's parse fails, the tokens are wrapped in
 * this record so that expression resolution can be deferred to code generation time.
 *
 * @param tokens the raw tokens forming the inline expression
 */
public record InlineExpr(@NotNull List<Token> tokens) {
}
