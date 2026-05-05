package dev.lumenlang.lumen.headless.sim.snapshot;

import dev.lumenlang.console.UIUtils;
import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.element.Renderer;
import dev.lumenlang.console.element.impl.widget.Tree;
import dev.lumenlang.console.style.Color;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders every captured {@link Snapshot} as a single {@link Tree} so a reviewer can read the
 * entire suite output in one block.
 */
public final class SnapshotRecapRenderer {

    private SnapshotRecapRenderer() {
    }

    /**
     * Prints one tree to {@code System.out} with one branch per snapshot in {@code snaps}.
     */
    public static void print(@NotNull List<Snapshot> snaps) {
        if (snaps.isEmpty()) return;
        System.out.println();
        System.out.println(Renderer.render(UIUtils.text("ALL SNAPSHOTS (" + snaps.size() + ")").fg(Color.BONE).bold()));
        Tree.Node[] nodes = new Tree.Node[snaps.size()];
        for (int i = 0; i < snaps.size(); i++) {
            nodes[i] = caseNode(snaps.get(i), i + 1, snaps.size());
        }
        System.out.println(Renderer.render(Tree.of(nodes)));
        System.out.println();
    }

    private static @NotNull Tree.Node caseNode(@NotNull Snapshot snap, int idx, int total) {
        Element label = UIUtils.row(
                UIUtils.text("[" + idx + "/" + total + "] ").fg(Color.SLATE),
                UIUtils.text(snap.caseName()).fg(Color.BONE).bold());
        List<Element> detail = new ArrayList<>();
        String envSummary = snap.env().isEmpty() ? "<empty>" : String.join("; ", snap.env());
        detail.add(UIUtils.row(
                UIUtils.text("input: ").fg(Color.SLATE),
                UIUtils.text(snap.input()).fg(Color.BONE),
                UIUtils.text("  env: " + envSummary).fg(Color.SLATE)));
        if (snap.suggestions().isEmpty()) {
            detail.add(UIUtils.text("no suggestions").fg(Color.GHOST_GREY));
        } else {
            for (int i = 0; i < snap.suggestions().size(); i++) {
                SuggestionSnap s = snap.suggestions().get(i);
                detail.add(UIUtils.row(
                        UIUtils.text("[#" + i + "] ").fg(Color.SLATE),
                        UIUtils.text(formatConfidence(s.confidence()) + "  ").fg(Color.MINT),
                        UIUtils.text(s.patternRaw()).fg(Color.BONE).bold()));
                for (String issue : s.issues()) {
                    detail.add(UIUtils.row(
                            UIUtils.text("       "),
                            UIUtils.text(issue).fg(Color.GHOST_GREY)));
                }
            }
        }
        return Tree.Node.leaf(label).withDetail(detail.toArray(new Element[0]));
    }

    private static @NotNull String formatConfidence(double v) {
        return String.format("%.3f", v);
    }
}
