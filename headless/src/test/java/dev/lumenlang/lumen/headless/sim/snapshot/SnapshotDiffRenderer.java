package dev.lumenlang.lumen.headless.sim.snapshot;

import dev.lumenlang.console.UIUtils;
import dev.lumenlang.console.element.Renderer;
import dev.lumenlang.console.style.Color;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders {@link SnapshotDiff} entries as full-context unified diffs so surrounding
 * fields stay visible alongside the changed lines.
 */
public final class SnapshotDiffRenderer {

    private SnapshotDiffRenderer() {
    }

    /**
     * Prints {@code diffs} to {@code System.out}, grouped by status with a header summary.
     */
    public static void print(@NotNull List<SnapshotDiff> diffs) {
        int newC = 0;
        int unchanged = 0;
        int changed = 0;
        for (SnapshotDiff d : diffs) {
            switch (d.status()) {
                case NEW -> newC++;
                case UNCHANGED -> unchanged++;
                case CHANGED -> changed++;
            }
        }
        System.out.println();
        System.out.println(Renderer.render(UIUtils.row(
                UIUtils.text("snapshot diff: ").fg(Color.BONE).bold(),
                UIUtils.text(unchanged + " unchanged ").fg(Color.MINT),
                UIUtils.text(changed + " changed ").fg(Color.WARM_YELLOW),
                UIUtils.text(newC + " new").fg(Color.SKY))));
        System.out.println();
        for (SnapshotDiff d : diffs) {
            if (d.status() == SnapshotDiff.Status.UNCHANGED) continue;
            renderCase(d);
        }
    }

    private static void renderCase(@NotNull SnapshotDiff d) {
        Color statusColor = d.status() == SnapshotDiff.Status.NEW ? Color.SKY : Color.WARM_YELLOW;
        System.out.println(Renderer.render(UIUtils.row(
                UIUtils.text("[" + d.status() + "] ").fg(statusColor).bold(),
                UIUtils.text(d.caseName()).fg(Color.BONE).bold())));
        if (d.status() == SnapshotDiff.Status.NEW) {
            renderAdded(SnapshotSerializer.serialize(d.actual()));
        } else if (d.baseline() != null) {
            renderUnified(SnapshotSerializer.serialize(d.baseline()), SnapshotSerializer.serialize(d.actual()));
        }
        System.out.println();
    }

    private static void renderAdded(@NotNull String text) {
        for (String line : text.split("\n", -1)) {
            if (line.isEmpty()) continue;
            System.out.println(Renderer.render(UIUtils.row(
                    UIUtils.text("  + ").fg(Color.MINT).bold(),
                    UIUtils.text(line).fg(Color.BONE))));
        }
    }

    private static void renderUnified(@NotNull String beforeText, @NotNull String afterText) {
        List<String> before = splitNonEmpty(beforeText);
        List<String> after = splitNonEmpty(afterText);
        for (DiffLine line : computeLcsDiff(before, after)) {
            switch (line.kind) {
                case CONTEXT -> System.out.println(Renderer.render(UIUtils.row(
                        UIUtils.text("    ").fg(Color.GHOST_GREY),
                        UIUtils.text(line.text).fg(Color.GHOST_GREY))));
                case REMOVED -> System.out.println(Renderer.render(UIUtils.row(
                        UIUtils.text("  - ").fg(Color.ALARM_RED).bold(),
                        UIUtils.text(line.text).fg(Color.BONE))));
                case ADDED -> System.out.println(Renderer.render(UIUtils.row(
                        UIUtils.text("  + ").fg(Color.MINT).bold(),
                        UIUtils.text(line.text).fg(Color.BONE))));
            }
        }
    }

    private static @NotNull List<String> splitNonEmpty(@NotNull String text) {
        List<String> out = new ArrayList<>();
        for (String line : text.split("\n", -1)) {
            if (!line.isEmpty()) out.add(line);
        }
        return out;
    }

    private static @NotNull List<DiffLine> computeLcsDiff(@NotNull List<String> a, @NotNull List<String> b) {
        int n = a.size();
        int m = b.size();
        int[][] dp = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                if (a.get(i).equals(b.get(j))) dp[i][j] = dp[i + 1][j + 1] + 1;
                else dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
            }
        }
        List<DiffLine> result = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < n && j < m) {
            if (a.get(i).equals(b.get(j))) {
                result.add(new DiffLine(DiffKind.CONTEXT, a.get(i)));
                i++;
                j++;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                result.add(new DiffLine(DiffKind.REMOVED, a.get(i)));
                i++;
            } else {
                result.add(new DiffLine(DiffKind.ADDED, b.get(j)));
                j++;
            }
        }
        while (i < n) result.add(new DiffLine(DiffKind.REMOVED, a.get(i++)));
        while (j < m) result.add(new DiffLine(DiffKind.ADDED, b.get(j++)));
        return result;
    }

    private enum DiffKind {
        CONTEXT, REMOVED, ADDED
    }

    private record DiffLine(@NotNull DiffKind kind, @NotNull String text) {
    }
}
