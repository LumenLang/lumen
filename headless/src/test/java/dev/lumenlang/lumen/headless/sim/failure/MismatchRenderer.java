package dev.lumenlang.lumen.headless.sim.failure;

import dev.lumenlang.console.UIUtils;
import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.style.Color;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a {@link Mismatch} as a small set of styled rows ready to be embedded under a tree node.
 */
public final class MismatchRenderer {

    private MismatchRenderer() {
    }

    /**
     * Rows for {@code mismatch}, each already styled.
     */
    public static @NotNull List<Element> render(@NotNull Mismatch mismatch) {
        List<Element> rows = new ArrayList<>();
        rows.add(header(mismatch.label()));
        if (mismatch instanceof TopPatternMismatch m) {
            rows.add(expectedRow(m.expected()));
            rows.add(actualRow(m.actual() == null ? "<no suggestion returned>" : m.actual()));
        } else if (mismatch instanceof PrimaryIssueMismatch m) {
            rows.add(expectedRow(m.expectedType()));
            rows.add(actualRow(formatIssueList(m.actualTypes())));
            if (m.customReason() != null) rows.add(reasonRow(m.customReason()));
        } else if (mismatch instanceof AnyIssueMismatch m) {
            rows.add(expectedRow("contains " + m.expectedType()));
            rows.add(actualRow(formatIssueList(m.actualTypes())));
        } else if (mismatch instanceof ConfidenceMismatch m) {
            rows.add(expectedRow(">= " + format3(m.minimum())));
            rows.add(actualRow(m.actual() == null ? "<no suggestion returned>" : format3(m.actual())));
        } else if (mismatch instanceof SuggestionCountMismatch m) {
            String range = m.min() == m.max() ? String.valueOf(m.min()) : m.min() + "..." + m.max();
            rows.add(expectedRow("count = " + range));
            rows.add(actualRow("count = " + m.actual()));
        } else if (mismatch instanceof SuggestionPresenceMismatch m) {
            rows.add(expectedRow(m.expectedNonEmpty() ? "at least 1 suggestion" : "no suggestions"));
            rows.add(actualRow("count = " + m.actualCount()));
        } else if (mismatch instanceof ContainsPatternMismatch m) {
            rows.add(expectedRow(m.expected()));
            if (m.actual().isEmpty()) {
                rows.add(actualRow("<no suggestions>"));
            } else {
                rows.add(actualRow("not present in:"));
                for (String pattern : m.actual()) {
                    rows.add(UIUtils.text("    - " + pattern).fg(Color.GHOST_GREY));
                }
            }
        } else if (mismatch instanceof CustomMismatch m) {
            rows.add(reasonRow(m.reason()));
        }
        return rows;
    }

    private static @NotNull Element header(@NotNull String label) {
        return UIUtils.text(label).fg(Color.ALARM_RED).bold();
    }

    private static @NotNull Element expectedRow(@NotNull String value) {
        return UIUtils.row(
                UIUtils.text("  expected  ").fg(Color.GHOST_GREY),
                UIUtils.text(value).fg(Color.BONE).bold()
        );
    }

    private static @NotNull Element actualRow(@NotNull String value) {
        return UIUtils.row(
                UIUtils.text("  got       ").fg(Color.GHOST_GREY),
                UIUtils.text(value).fg(Color.BONE).bold()
        );
    }

    private static @NotNull Element reasonRow(@NotNull String reason) {
        return UIUtils.row(
                UIUtils.text("  reason    ").fg(Color.GHOST_GREY),
                UIUtils.text(reason).fg(Color.GHOST_GREY)
        );
    }

    private static @NotNull String formatIssueList(@NotNull List<String> issues) {
        if (issues.isEmpty()) return "<no issues>";
        return String.join(" + ", issues);
    }

    private static @NotNull String format3(double value) {
        return String.format("%.3f", value);
    }
}
