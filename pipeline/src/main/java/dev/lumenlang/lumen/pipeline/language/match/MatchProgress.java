package dev.lumenlang.lumen.pipeline.language.match;

import dev.lumenlang.lumen.pipeline.language.pattern.PatternPart;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Tracks the progress and failure point of a pattern match attempt.
 *
 * <p>During matching, the matcher records the deepest point reached before
 * failure across all backtracking branches. After matching completes, this
 * object describes either a successful match or exactly where and why
 * the best attempt failed.
 */
public final class MatchProgress {

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
}
