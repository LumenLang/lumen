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
 * Tracks the progress and failure point of a pattern match attempt.
 *
 * <p>During matching, the matcher records the deepest point reached before
 * failure across all backtracking branches. After matching completes, this
 * object describes either a successful match or exactly where and why
 * the best attempt failed.
 *
 * <p>In addition to the single deepest failure, this class accumulates a list
 * of all type binding failures discovered during continuation matching. This
 * allows diagnostics to report multiple binding failures at once.
 */
public final class MatchProgress {

    private final @NotNull List<BindingFailure> bindingFailures = new ArrayList<>();
    private final @NotNull List<LiteralTypo> literalTypos = new ArrayList<>();
    private @Nullable Match match;
    private int furthestTokenIndex = -1;
    private @Nullable PatternPart failedPart;
    private @Nullable String failedBindingId;
    private @Nullable String failedReason;
    private @NotNull List<Token> failedTokens = List.of();
    private @Nullable String lastRejectionReason;

    void storeRejectionReason(@NotNull String reason) {
        this.lastRejectionReason = reason;
    }

    void clearRejectionReason() {
        this.lastRejectionReason = null;
    }

    void recordFailure(int tokenIndex, @Nullable PatternPart part, @Nullable String bindingId, @NotNull List<Token> failedTokens) {
        if (tokenIndex > this.furthestTokenIndex) {
            if (bindingId == null && this.failedBindingId != null && tokenIndex - this.furthestTokenIndex <= 2) return;
            this.furthestTokenIndex = tokenIndex;
            this.failedPart = part;
            this.failedBindingId = bindingId;
            this.failedTokens = failedTokens;
            this.failedReason = this.lastRejectionReason;
        }
        this.lastRejectionReason = null;
    }

    void recordBindingFailure(int tokenIndex, @NotNull String bindingId, @NotNull List<Token> failedTokens) {
        bindingFailures.add(new BindingFailure(tokenIndex, bindingId, lastRejectionReason, failedTokens));
    }

    void transferBindingFailures(@NotNull MatchProgress from) {
        bindingFailures.addAll(from.bindingFailures);
        literalTypos.addAll(from.literalTypos);
    }

    void recordLiteralTypo(@NotNull Token token, @NotNull String expected) {
        literalTypos.add(new LiteralTypo(token, expected));
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
     * @return the deepest token index reached before failure across all backtracking branches, or -1 if nothing was consumed
     */
    public int furthestTokenIndex() {
        return furthestTokenIndex;
    }

    /**
     * @return the pattern part that was being matched when the deepest failure occurred, or null
     */
    public @Nullable PatternPart failedPart() {
        return failedPart;
    }

    /**
     * @return the type binding ID that rejected the input, or null if the failure was not binding related
     */
    public @Nullable String failedBindingId() {
        return failedBindingId;
    }

    /**
     * @return a human readable reason why the type binding rejected the input, or null if unknown
     */
    public @Nullable String failedReason() {
        return failedReason;
    }

    /**
     * @return the tokens that were attempted when the deepest failure occurred
     */
    public @NotNull List<Token> failedTokens() {
        return failedTokens;
    }

    /**
     * Returns all type binding failures discovered during continuation matching,
     * deduplicated by binding ID (keeping the failure at the highest token index
     * for each binding).
     *
     * @return an immutable list of binding failures, one per unique binding ID
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
     * Represents a single type binding failure during pattern matching.
     *
     * @param tokenIndex   the token index where the binding was attempted
     * @param bindingId    the type binding identifier that rejected the input
     * @param reason       a human readable rejection reason, or null
     * @param failedTokens the tokens that were attempted
     */
    public record BindingFailure(int tokenIndex, @NotNull String bindingId, @Nullable String reason,
                                 @NotNull List<Token> failedTokens) {
    }

    /**
     * Represents a literal token that was close enough to a pattern literal to be considered a typo.
     *
     * @param token    the mismatched token
     * @param expected the literal text the token was close to
     */
    public record LiteralTypo(@NotNull Token token, @NotNull String expected) {
    }
}
