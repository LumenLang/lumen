package dev.lumenlang.lumen.pipeline.language.match;

import dev.lumenlang.lumen.pipeline.language.pattern.PatternPart;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks the progress and failure point of a single pattern match attempt.
 *
 * <p>During matching, the deepest position reached across all backtracking
 * branches is recorded as the {@linkplain #deepestFailure() deepest failure}.
 * Per binding rejections encountered during downstream discovery are
 * collected separately as {@link BindingFailure} entries.
 *
 * <p>Failures are produced exclusively from explicit rejection messages
 * supplied by type bindings, literals, and structural matchers. Reasons
 * therefore always carry a precise, binding authored explanation.
 */
public final class MatchProgress {

    private final @NotNull List<BindingFailure> bindingFailures = new ArrayList<>();
    private final @NotNull List<LiteralTypo> literalTypos = new ArrayList<>();
    private @NotNull List<Token> unmatchedTrailingTokens = List.of();
    private @Nullable Match match;
    private @Nullable Failure deepest;
    private @Nullable Incomplete incomplete;

    void recordFailure(int tokenIndex, @Nullable PatternPart part, @Nullable String bindingId, @Nullable String reason, @NotNull List<Token> failedTokens) {
        if (deepest != null && tokenIndex <= deepest.tokenIndex()) return;
        if (deepest != null && bindingId == null && deepest.bindingId() != null && tokenIndex - deepest.tokenIndex() <= 2)
            return;
        deepest = new Failure(tokenIndex, part, bindingId, reason, failedTokens);
    }

    void recordBindingFailure(int tokenIndex, @NotNull String bindingId, @NotNull String reason, @NotNull List<Token> failedTokens) {
        bindingFailures.add(new BindingFailure(tokenIndex, bindingId, reason, failedTokens));
    }

    void transferBindingFailures(@NotNull MatchProgress from) {
        bindingFailures.addAll(from.bindingFailures);
        literalTypos.addAll(from.literalTypos);
    }

    void recordLiteralTypo(@NotNull Token token, @NotNull String expected) {
        literalTypos.add(new LiteralTypo(token, expected));
    }

    void recordUnmatchedTrailingTokens(@NotNull List<Token> tokens) {
        if (tokens.size() > unmatchedTrailingTokens.size()) {
            unmatchedTrailingTokens = List.copyOf(tokens);
        }
    }

    void recordIncomplete(int afterTokenIndex, @NotNull String expectedNext) {
        if (incomplete == null || afterTokenIndex > incomplete.afterTokenIndex()) {
            incomplete = new Incomplete(afterTokenIndex, expectedNext);
        }
    }

    void recordSuccess(@NotNull Match match) {
        this.match = match;
    }

    /**
     * @return true if the pattern matched successfully
     */
    public boolean succeeded() {
        return match != null;
    }

    /**
     * @return the successful match result, or null if matching failed
     */
    public @Nullable Match match() {
        return match;
    }

    /**
     * @return the deepest failure observed across all backtracking branches, or null when nothing was attempted
     */
    public @Nullable Failure deepestFailure() {
        return deepest;
    }

    /**
     * @return the deepest token index reached before failure, or {@code -1} when nothing was attempted
     */
    public int furthestTokenIndex() {
        return deepest == null ? -1 : deepest.tokenIndex();
    }

    /**
     * @return the pattern part that was being matched at the deepest failure, or null when not available
     */
    public @Nullable PatternPart failedPart() {
        return deepest == null ? null : deepest.part();
    }

    /**
     * @return the type binding id that produced the deepest failure, or null when the deepest failure was a literal or structural mismatch
     */
    public @Nullable String failedBindingId() {
        return deepest == null ? null : deepest.bindingId();
    }

    /**
     * @return the human readable reason from the deepest failure, or null when the failure originated from a literal or structural match (which has no binding authored message)
     */
    public @Nullable String failedReason() {
        return deepest == null ? null : deepest.reason();
    }

    /**
     * @return the tokens attempted when the deepest failure occurred
     */
    public @NotNull List<Token> failedTokens() {
        return deepest == null ? List.of() : deepest.failedTokens();
    }

    /**
     * @return tokens that remained unmatched after all pattern parts were consumed during continuation
     */
    public @NotNull List<Token> unmatchedTrailingTokens() {
        return unmatchedTrailingTokens;
    }

    /**
     * Returns all type binding failures discovered during continuation matching,
     * deduplicated by binding id (keeping the failure at the highest token index
     * for each binding).
     *
     * @return immutable list of binding failures, one per unique binding id
     */
    public @NotNull List<BindingFailure> bindingFailures() {
        if (bindingFailures.isEmpty()) return List.of();
        Map<String, BindingFailure> best = new LinkedHashMap<>();
        for (BindingFailure bf : bindingFailures) {
            BindingFailure existing = best.get(bf.bindingId());
            if (existing == null || bf.tokenIndex() > existing.tokenIndex()) {
                best.put(bf.bindingId(), bf);
            }
        }
        return List.copyOf(best.values());
    }

    /**
     * @return non-null when the matcher exhausted input mid-pattern still expecting more content
     */
    public @Nullable Incomplete incomplete() {
        return incomplete;
    }

    /**
     * @return all literal typos discovered during downstream failure analysis, deduplicated by token position
     */
    public @NotNull List<LiteralTypo> literalTypos() {
        if (literalTypos.isEmpty()) return List.of();
        Map<Integer, LiteralTypo> seen = new LinkedHashMap<>();
        for (LiteralTypo lt : literalTypos) {
            seen.putIfAbsent(lt.token().start(), lt);
        }
        return List.copyOf(seen.values());
    }

    /**
     * Snapshot of the deepest failure observed during a single match attempt.
     *
     * @param tokenIndex   the token index at which the failure occurred
     * @param part         the pattern part being matched, or null when unavailable
     * @param bindingId    the type binding id when the failure was binding driven, or null
     * @param reason       a human readable reason from the binding, or null when the failure was a literal or structural mismatch
     * @param failedTokens the tokens attempted at the failure point
     */
    public record Failure(int tokenIndex, @Nullable PatternPart part, @Nullable String bindingId,
                          @Nullable String reason, @NotNull List<Token> failedTokens) {
    }

    /**
     * A single type binding rejection captured during downstream failure analysis.
     *
     * @param tokenIndex   the token index where the binding was attempted
     * @param bindingId    the type binding id that rejected the input
     * @param reason       a human readable rejection reason supplied by the binding
     * @param failedTokens the tokens attempted at the rejection point
     */
    public record BindingFailure(int tokenIndex, @NotNull String bindingId, @NotNull String reason,
                                 @NotNull List<Token> failedTokens) {
    }

    /**
     * A literal token close enough to a pattern literal to be treated as a typo.
     *
     * @param token    the mismatched token
     * @param expected the literal text the token was close to
     */
    public record LiteralTypo(@NotNull Token token, @NotNull String expected) {
    }

    /**
     * Records that the matcher exhausted input while still expecting more pattern content.
     *
     * @param afterTokenIndex token index after which input ran out (input length when no tokens
     *                        consumed at all)
     * @param expectedNext   short label for what the pattern expected next (literal text or
     *                        binding id)
     */
    public record Incomplete(int afterTokenIndex, @NotNull String expectedNext) {
    }
}
