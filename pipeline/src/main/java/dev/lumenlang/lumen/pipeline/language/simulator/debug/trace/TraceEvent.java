package dev.lumenlang.lumen.pipeline.language.simulator.debug.trace;

import dev.lumenlang.lumen.pipeline.language.match.MatchProgress;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.Suggestion;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.ScoreBreakdown;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Structured event emitted by the pattern simulator to a {@link SimulatorTracer}.
 */
public sealed interface TraceEvent {

    /**
     * One pre-filter pass result for a registered pattern.
     *
     * @param pattern   the candidate pattern
     * @param admitted  {@code true} when the pattern survived the minimum confidence cut
     * @param breakdown component scores that produced the pre-filter confidence
     */
    record CandidateScored(@NotNull Pattern pattern, boolean admitted,
                           @NotNull ScoreBreakdown breakdown) implements TraceEvent {
    }

    /**
     * A candidate produced a viable suggestion in the analysis phase.
     *
     * @param pattern    the candidate pattern
     * @param confidence analysis-phase confidence
     * @param stage      analysis branch label (level-0 typo, BFS-k extra, reorder,
     *                   type-mismatch fallback, etc)
     * @param issues     issues attached to the suggestion
     */
    record SuggestionFormed(@NotNull Pattern pattern, double confidence, @NotNull String stage,
                            @NotNull List<SuggestionIssue> issues) implements TraceEvent {
    }

    /**
     * One BFS removal level finished for a candidate.
     *
     * @param pattern              the candidate pattern
     * @param level                BFS removal depth
     * @param combinationsExplored combinations of token removals actually tried
     * @param succeeded            {@code true} when at least one combination matched
     */
    record BfsLevel(@NotNull Pattern pattern, int level, int combinationsExplored,
                    boolean succeeded) implements TraceEvent {
    }

    /**
     * An input token was treated as a typo of an expected literal.
     *
     * @param pattern  candidate pattern containing the literal
     * @param input    original input token
     * @param expected literal text the typo was corrected to
     * @param distance edit distance between input text and expected literal
     */
    record TypoConsidered(@NotNull Pattern pattern, @NotNull Token input, @NotNull String expected,
                          int distance) implements TraceEvent {
    }

    /**
     * Final ordered list of suggestions returned to the caller.
     *
     * @param ordered ranking-ordered suggestion list
     */
    record Ranked(@NotNull List<Suggestion> ordered) implements TraceEvent {
    }

    /**
     * Wall time spent in a simulator stage.
     *
     * @param stage  stage label
     * @param millis wall time in milliseconds
     */
    record StageTiming(@NotNull String stage, long millis) implements TraceEvent {
    }

    /**
     * A pattern was rejected by the pre-filter pass with the recorded reason.
     */
    record PreFilterRejected(@NotNull Pattern pattern,
                             @NotNull String reason) implements TraceEvent {
    }

    /**
     * One {@code matchWithProgress} call completed against a candidate pattern.
     *
     * @param pattern  the candidate pattern
     * @param stage    label describing which sub-step issued the call (level-0, BFS-k extra,
     *                 typo-corrected, shape-match, partial-typo recheck)
     * @param progress full match progress including failed bindings, literal typos, and trailing
     *                 tokens
     */
    record MatchAttempt(@NotNull Pattern pattern, @NotNull String stage,
                        @NotNull MatchProgress progress) implements TraceEvent {
    }

    /**
     * One literal-form distance check during the pre-filter pass.
     *
     * @param pattern    candidate pattern owning the literal
     * @param tokenIndex input token index tested
     * @param tokenText  input token text
     * @param form       literal form text
     * @param distance   prefix-aware edit distance
     * @param threshold  distance threshold derived from token and form lengths
     * @param accepted   {@code true} when the form was within the threshold
     */
    record LiteralProbe(@NotNull Pattern pattern, int tokenIndex, @NotNull String tokenText,
                        @NotNull String form, int distance, int threshold,
                        boolean accepted) implements TraceEvent {
    }

    /**
     * One typo-fix candidate considered during {@code findBestTypoFix}.
     *
     * @param pattern    candidate pattern
     * @param tokenIndex input token index
     * @param tokenText  input token text
     * @param form       literal form proposed as the correction
     * @param distance   distance between {@code tokenText} and {@code form}
     * @param threshold  threshold required to accept
     * @param keptAsBest {@code true} when this candidate became the new best fix at the time
     */
    record TypoCandidate(@NotNull Pattern pattern, int tokenIndex, @NotNull String tokenText,
                         @NotNull String form, int distance, int threshold,
                         boolean keptAsBest) implements TraceEvent {
    }

    /**
     * The shape-match step constructed and matched a token sequence for the reorder fallback.
     *
     * @param pattern  the candidate pattern
     * @param shaped   the synthesised token sequence fed to the matcher
     * @param progress match progress against the synthesised sequence, or {@code null} when shape
     *                 construction produced no sequence
     */
    record ShapeAttempt(@NotNull Pattern pattern, @NotNull List<Token> shaped,
                        @Nullable MatchProgress progress) implements TraceEvent {
    }

    /**
     * One BFS combination explored within a removal level.
     *
     * @param pattern        candidate pattern
     * @param level          BFS removal depth
     * @param removedIndices token indices removed in this combination
     * @param matched        {@code true} when the reduced sequence matched
     * @param furthestIndex  furthest input token index reached by the matcher
     */
    record BfsCombination(@NotNull Pattern pattern, int level, @NotNull int[] removedIndices,
                          boolean matched, int furthestIndex) implements TraceEvent {
    }
}

