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
     * @param stage    label describing which sub-step issued the call (level-0, typo-corrected,
     *                 partial-typo recheck)
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
     * Handler sandbox invocation rejected the candidate by throwing.
     *
     * @param pattern   candidate pattern whose handler ran
     * @param stage     label describing which sim stage triggered the sandbox call
     * @param throwable exception thrown by the handler
     */
    record SandboxRejected(@NotNull Pattern pattern, @NotNull String stage,
                           @NotNull Throwable throwable) implements TraceEvent {
    }
}


