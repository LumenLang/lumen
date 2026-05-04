package dev.lumenlang.lumen.headless.sim.snapshot;

import dev.lumenlang.console.UIUtils;
import dev.lumenlang.console.element.Renderer;
import dev.lumenlang.console.style.Color;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Renders one or more {@link SnapshotDiff} entries as compact, colored, git-style output.
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
            renderNew(d.actual());
        } else {
            for (SnapshotDiff.FieldChange c : d.fieldChanges()) {
                renderChange(c);
            }
        }
        System.out.println();
    }

    private static void renderNew(@NotNull Snapshot snap) {
        System.out.println(Renderer.render(UIUtils.row(
                UIUtils.text("  input: ").fg(Color.SLATE),
                UIUtils.text(snap.input()).fg(Color.BONE))));
        if (snap.suggestions().isEmpty()) {
            System.out.println(Renderer.render(UIUtils.text("  no suggestions").fg(Color.GHOST_GREY)));
            return;
        }
        for (int i = 0; i < snap.suggestions().size(); i++) {
            SuggestionSnap s = snap.suggestions().get(i);
            System.out.println(Renderer.render(UIUtils.row(
                    UIUtils.text("  [#" + i + "] ").fg(Color.SLATE),
                    UIUtils.text(s.patternRaw()).fg(Color.BONE).bold(),
                    UIUtils.text("  conf=" + String.format("%.3f", s.confidence())).fg(Color.MINT))));
            for (String issue : s.issues()) {
                System.out.println(Renderer.render(UIUtils.row(
                        UIUtils.text("        "),
                        UIUtils.text(issue).fg(Color.GHOST_GREY))));
            }
        }
    }

    private static void renderChange(@NotNull SnapshotDiff.FieldChange c) {
        System.out.println(Renderer.render(UIUtils.text("  " + c.label()).fg(Color.SLATE)));
        if (c.before() != null) {
            System.out.println(Renderer.render(UIUtils.row(
                    UIUtils.text("  - ").fg(Color.ALARM_RED).bold(),
                    UIUtils.text(c.before()).fg(Color.BONE))));
        }
        if (c.after() != null) {
            System.out.println(Renderer.render(UIUtils.row(
                    UIUtils.text("  + ").fg(Color.MINT).bold(),
                    UIUtils.text(c.after()).fg(Color.BONE))));
        }
    }
}
