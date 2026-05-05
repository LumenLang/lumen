package dev.lumenlang.lumen.headless.sim.snapshot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the {@link SnapshotDiff} between an actual {@link Snapshot} and its stored baseline.
 */
public final class SnapshotComparator {

    private SnapshotComparator() {
    }

    /**
     * Builds a {@link SnapshotDiff} comparing {@code actual} against {@code baseline}.
     */
    public static @NotNull SnapshotDiff compare(@Nullable Snapshot baseline, @NotNull Snapshot actual) {
        if (baseline == null) {
            return new SnapshotDiff(actual.caseName(), SnapshotDiff.Status.NEW, List.of(), null, actual);
        }
        List<SnapshotDiff.FieldChange> changes = new ArrayList<>();
        if (!baseline.input().equals(actual.input())) {
            changes.add(new SnapshotDiff.FieldChange("input", baseline.input(), actual.input()));
        }
        if (!baseline.runner().equals(actual.runner())) {
            changes.add(new SnapshotDiff.FieldChange("runner", baseline.runner(), actual.runner()));
        }
        if (!baseline.env().equals(actual.env())) {
            changes.add(new SnapshotDiff.FieldChange("env", String.join("; ", baseline.env()), String.join("; ", actual.env())));
        }
        compareSuggestions(baseline.suggestions(), actual.suggestions(), changes);
        SnapshotDiff.Status status = changes.isEmpty() ? SnapshotDiff.Status.UNCHANGED : SnapshotDiff.Status.CHANGED;
        return new SnapshotDiff(actual.caseName(), status, changes, baseline, actual);
    }

    private static void compareSuggestions(@NotNull List<SuggestionSnap> base, @NotNull List<SuggestionSnap> act, @NotNull List<SnapshotDiff.FieldChange> changes) {
        int max = Math.max(base.size(), act.size());
        for (int i = 0; i < max; i++) {
            SuggestionSnap b = i < base.size() ? base.get(i) : null;
            SuggestionSnap a = i < act.size() ? act.get(i) : null;
            String prefix = "[#" + i + "]";
            if (b == null) {
                changes.add(new SnapshotDiff.FieldChange(prefix + " (new suggestion)", null, summary(a)));
                continue;
            }
            if (a == null) {
                changes.add(new SnapshotDiff.FieldChange(prefix + " (removed suggestion)", summary(b), null));
                continue;
            }
            if (!b.patternRaw().equals(a.patternRaw())) {
                changes.add(new SnapshotDiff.FieldChange(prefix + " pattern", b.patternRaw(), a.patternRaw()));
            }
            if (Math.abs(b.confidence() - a.confidence()) > 1e-6) {
                changes.add(new SnapshotDiff.FieldChange(prefix + " confidence", confidence(b), confidence(a)));
            }
            if (!b.issues().equals(a.issues())) {
                changes.add(new SnapshotDiff.FieldChange(prefix + " issues", issuesLine(b), issuesLine(a)));
            }
        }
    }

    private static @NotNull String summary(@NotNull SuggestionSnap s) {
        return s.patternRaw() + " (conf=" + confidence(s) + ", " + (s.issues().isEmpty() ? "no issues" : s.issues().size() + " issue(s)") + ")";
    }

    private static @NotNull String confidence(@NotNull SuggestionSnap s) {
        return String.format("%.3f", s.confidence());
    }

    private static @NotNull String issuesLine(@NotNull SuggestionSnap s) {
        return s.issues().isEmpty() ? "<none>" : String.join(" | ", s.issues());
    }
}
