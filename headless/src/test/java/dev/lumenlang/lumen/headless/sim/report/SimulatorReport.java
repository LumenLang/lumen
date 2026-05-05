package dev.lumenlang.lumen.headless.sim.report;

import dev.lumenlang.lumen.headless.sim.snapshot.BaselineStore;
import dev.lumenlang.lumen.headless.sim.snapshot.Snapshot;
import dev.lumenlang.lumen.headless.sim.snapshot.SnapshotComparator;
import dev.lumenlang.lumen.headless.sim.snapshot.SnapshotDiff;
import dev.lumenlang.lumen.headless.sim.snapshot.SnapshotDiffRenderer;
import dev.lumenlang.lumen.headless.sim.snapshot.SnapshotRecapRenderer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Collects {@link Snapshot} captures during a test run and renders the snapshot diff once the
 * suite finishes. Default mode compares against stored baselines and fails on any diff;
 * {@code -Psim.baseline=true} overwrites every baseline instead.
 */
public final class SimulatorReport implements AfterAllCallback {

    private static final List<Snapshot> CAPTURED = new ArrayList<>();

    /**
     * Records {@code snap} for end-of-run comparison or baseline write.
     */
    public static synchronized void record(@NotNull Snapshot snap) {
        CAPTURED.add(snap);
    }

    private static int countChanged(@NotNull List<SnapshotDiff> diffs) {
        int n = 0;
        for (SnapshotDiff d : diffs) if (d.status() != SnapshotDiff.Status.UNCHANGED) n++;
        return n;
    }

    @Override
    public void afterAll(@NotNull ExtensionContext context) {
        if (CAPTURED.isEmpty()) return;
        boolean baseline = Boolean.parseBoolean(System.getProperty("sim.baseline", "false"));
        if (baseline) {
            BaselineStore.clear();
            for (Snapshot snap : CAPTURED) BaselineStore.write(snap);
            CAPTURED.sort(Comparator.comparing(Snapshot::caseName));
            SnapshotRecapRenderer.print(CAPTURED);
            System.out.println("wrote " + CAPTURED.size() + " baseline snapshot(s)");
            CAPTURED.clear();
            return;
        }
        List<SnapshotDiff> diffs = new ArrayList<>(CAPTURED.size());
        boolean anyDiff = false;
        for (Snapshot snap : CAPTURED) {
            Snapshot stored = BaselineStore.read(snap.caseName());
            SnapshotDiff diff = SnapshotComparator.compare(stored, snap);
            diffs.add(diff);
            if (diff.status() != SnapshotDiff.Status.UNCHANGED) anyDiff = true;
        }
        SnapshotDiffRenderer.print(diffs);
        CAPTURED.clear();
        if (anyDiff) {
            throw new AssertionError("snapshot diff: " + countChanged(diffs) + " case(s) differ from baseline (run with -Psim.baseline=true to accept)");
        }
    }
}
