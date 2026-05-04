package dev.lumenlang.lumen.headless.sim.snapshot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Outcome of comparing an actual snapshot against its stored baseline.
 *
 * @param caseName     name of the case the diff describes
 * @param status       NEW, UNCHANGED, or CHANGED
 * @param fieldChanges per-field changes when {@code status} is CHANGED, otherwise empty
 * @param baseline     stored baseline, or {@code null} for NEW
 * @param actual       observed snapshot
 */
public record SnapshotDiff(@NotNull String caseName, @NotNull Status status,
                           @NotNull List<FieldChange> fieldChanges, @Nullable Snapshot baseline,
                           @NotNull Snapshot actual) {

    /**
     * Outcome category for a single case.
     */
    public enum Status {

        /**
         * No baseline existed, the actual snapshot was just written.
         */
        NEW,

        /**
         * Baseline matches actual exactly.
         */
        UNCHANGED,

        /**
         * Baseline and actual differ in at least one field.
         */
        CHANGED
    }

    /**
     * One labelled before-after diff entry produced by {@link SnapshotComparator}.
     *
     * @param label  short human label, e.g. {@code "[#0] confidence"}
     * @param before previous value, or {@code null} when the field was added
     * @param after  current value, or {@code null} when the field was removed
     */
    public record FieldChange(@NotNull String label, @Nullable String before,
                              @Nullable String after) {
    }
}
