package dev.lumenlang.lumen.pipeline.language.simulator.debug;

import dev.lumenlang.lumen.pipeline.language.match.MatchProgress;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.trace.TraceEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Centralises debug emission. Each method gates on verbosity once and emits both a structured
 * trace event and a formatted sink line.
 */
public final class Trace {

    private Trace() {
    }

    public static void preFilterReject(@NotNull SimulatorDebug debug, @NotNull Pattern pattern, @NotNull String reason) {
        debug.trace(new TraceEvent.PreFilterRejected(pattern, reason));
        debug.emit(Verbosity.CANDIDATES, 1, () -> "- " + pattern.raw() + "  rejected: " + reason);
    }

    public static void literalProbe(@NotNull SimulatorDebug debug, @NotNull Pattern pattern, int tokenIndex, @NotNull String tokenText, @NotNull String form, int distance, int threshold, boolean accepted) {
        if (!debug.enabled(Verbosity.DEEP)) return;
        debug.trace(new TraceEvent.LiteralProbe(pattern, tokenIndex, tokenText, form, distance, threshold, accepted));
        debug.emit(Verbosity.DEEP, 3, () -> "probe tok#" + tokenIndex + " '" + tokenText + "' vs form '" + form + "' dist=" + distance + " thr=" + threshold + " " + (accepted ? "ACCEPT" : "skip"));
    }

    public static void typoCandidate(@NotNull SimulatorDebug debug, @NotNull Pattern pattern, int tokenIndex, @NotNull String tokenText, @NotNull String form, int distance, int threshold, boolean keptAsBest) {
        if (!debug.enabled(Verbosity.DEEP)) return;
        debug.trace(new TraceEvent.TypoCandidate(pattern, tokenIndex, tokenText, form, distance, threshold, keptAsBest));
        debug.emit(Verbosity.DEEP, 3, () -> "typo? tok#" + tokenIndex + " '" + tokenText + "' -> '" + form + "' dist=" + distance + " thr=" + threshold + (keptAsBest ? " *best*" : ""));
    }

    public static void matchAttempt(@NotNull SimulatorDebug debug, @NotNull Pattern pattern, @NotNull String stage, @NotNull MatchProgress progress) {
        debug.trace(new TraceEvent.MatchAttempt(pattern, stage, progress));
        if (!debug.enabled(Verbosity.BIND)) return;
        debug.emit(Verbosity.BIND, 3, () -> stage + " match: " + (progress.succeeded() ? "OK" : "FAIL") + " furthest=" + progress.furthestTokenIndex() + (progress.failedBindingId() != null ? " failedBinding=" + progress.failedBindingId() : "") + (progress.failedReason() != null ? " reason=" + progress.failedReason() : ""));
        for (MatchProgress.BindingFailure bf : progress.bindingFailures()) {
            debug.emit(Verbosity.BIND, 4, () -> "binding " + bf.bindingId() + " failed: " + bf.reason() + " (failedTokens=" + bf.failedTokens().size() + ")");
        }
        for (MatchProgress.LiteralTypo lt : progress.literalTypos()) {
            debug.emit(Verbosity.BIND, 4, () -> "literalTypo: '" + lt.token().text() + "' expected '" + lt.expected() + "'");
        }
        if (!progress.unmatchedTrailingTokens().isEmpty()) {
            debug.emit(Verbosity.BIND, 4, () -> "unmatched trailing: " + progress.unmatchedTrailingTokens().size() + " tok(s)");
        }
    }

    public static void deep(@NotNull SimulatorDebug debug, @NotNull Supplier<String> line) {
        debug.emit(Verbosity.DEEP, 3, line);
    }

    public static void timing(@NotNull SimulatorDebug debug, @NotNull String stage, long nanos) {
        long ms = nanos / 1_000_000L;
        debug.trace(new TraceEvent.StageTiming(stage, ms));
        debug.emit(Verbosity.TIMING, 1, () -> stage + " " + ms + " ms (" + nanos + " ns)");
    }

    public static void deepTiming(@NotNull SimulatorDebug debug, @NotNull String stage, long nanos) {
        long us = nanos / 1_000L;
        debug.trace(new TraceEvent.StageTiming(stage, nanos / 1_000_000L));
        debug.emit(Verbosity.DEEP_TIMING, 2, () -> stage + " " + us + " us (" + nanos + " ns)");
    }

    public static void sandboxRejected(@NotNull SimulatorDebug debug, @NotNull Pattern pattern, @NotNull String stage, @NotNull Throwable thrown) {
        debug.trace(new TraceEvent.SandboxRejected(pattern, stage, thrown));
        if (!debug.enabled(Verbosity.BIND)) return;
        String type = thrown.getClass().getSimpleName();
        String msg = thrown.getMessage();
        debug.emit(Verbosity.BIND, 4, () -> "sandbox rejected (" + stage + "): " + type + (msg == null ? "" : ": " + msg));
        if (debug.enabled(Verbosity.DEEP)) {
            StackTraceElement[] trace = thrown.getStackTrace();
            int shown = Math.min(trace.length, 6);
            for (int i = 0; i < shown; i++) {
                StackTraceElement el = trace[i];
                debug.emit(Verbosity.DEEP, 5, () -> "at " + el);
            }
        }
    }
}
