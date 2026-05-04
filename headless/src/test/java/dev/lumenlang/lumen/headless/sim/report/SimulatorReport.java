package dev.lumenlang.lumen.headless.sim.report;

import dev.lumenlang.console.UIUtils;
import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.element.Renderer;
import dev.lumenlang.console.element.impl.widget.Tree;
import dev.lumenlang.console.style.Color;
import dev.lumenlang.console.style.Style;
import dev.lumenlang.lumen.headless.sim.failure.Mismatch;
import dev.lumenlang.lumen.headless.sim.failure.MismatchRenderer;
import dev.lumenlang.lumen.headless.sim.result.SimulatorRunRecord;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.Suggestion;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects every {@link SimulatorRunRecord} from a JUnit run and prints a unified tree summary
 * once all cases finish.
 */
public final class SimulatorReport implements AfterAllCallback {

    private static final List<SimulatorRunRecord> RECORDS = Collections.synchronizedList(new ArrayList<>());

    /**
     * Appends {@code record} to the report.
     */
    public static void record(@NotNull SimulatorRunRecord record) {
        RECORDS.add(record);
    }

    private static void print(@NotNull List<SimulatorRunRecord> records) {
        long passed = records.stream().filter(SimulatorRunRecord::passed).count();
        long failed = records.size() - passed;
        boolean observing = Boolean.getBoolean("sim.observe");

        List<Tree.Node> caseNodes = new ArrayList<>();
        for (SimulatorRunRecord record : records) {
            caseNodes.add(buildCaseNode(record, observing));
        }

        Tree tree = UIUtils.tree(caseNodes.toArray(Tree.Node[]::new))
                .title(suiteTitle(records.size(), passed, failed, observing))
                .connectorStyle(Style.fg(Color.SLATE).dim());

        System.out.println();
        System.out.println(Renderer.render(tree));
    }

    private static @NotNull Element suiteTitle(long total, long passed, long failed, boolean observing) {
        StringBuilder sb = new StringBuilder("Pattern simulator ran on ");
        sb.append(total).append(total == 1 ? " case" : " cases");
        if (observing) {
            sb.append(", observe mode (assertions ignored)");
        } else if (failed == 0) {
            sb.append(", all passed");
        } else {
            sb.append(", ").append(passed).append(" passed and ").append(failed).append(" failed");
        }
        return UIUtils.text(sb.toString()).fg(Color.BONE).bold();
    }

    private static @NotNull Tree.Node buildCaseNode(@NotNull SimulatorRunRecord record, boolean observing) {
        List<Element> details = new ArrayList<>();

        details.add(detail("input", record.input().isEmpty() ? "<empty>" : record.input()));

        if (record.suggestions().isEmpty()) {
            details.add(noteLine("simulator returned no suggestions, the input matched cleanly"));
        } else {
            details.add(noteLine(record.suggestions().size() == 1
                    ? "simulator ranked 1 near match"
                    : "simulator ranked " + record.suggestions().size() + " near matches, best first"));
            for (int i = 0; i < record.suggestions().size(); i++) {
                Suggestion s = record.suggestions().get(i);
                details.add(candidateLine(i, s));
                for (SuggestionIssue issue : s.issues()) {
                    details.add(issueLine(issue));
                }
            }
        }

        if (!observing && !record.mismatches().isEmpty()) {
            details.add(blank());
            details.add(noteLine("the case asserted things the simulator did not produce:").fg(Color.ALARM_RED));
            for (Mismatch mismatch : record.mismatches()) {
                for (Element row : MismatchRenderer.render(mismatch)) {
                    details.add(indent(row));
                }
            }
        }

        if (!record.recommendations().isEmpty()) {
            details.add(blank());
            details.add(noteLine("paste these expectations into the case to catch a regression later").fg(Color.SOFT_PEACH));
            for (String rec : record.recommendations()) {
                details.add(indent(UIUtils.text(rec).fg(Color.MINT)));
            }
        }

        return Tree.Node.leaf(caseLabel(record, observing)).withDetail(details.toArray(Element[]::new));
    }

    private static @NotNull Element detail(@NotNull String label, @NotNull String value) {
        return UIUtils.row(
                UIUtils.text(label + "  ").fg(Color.GHOST_GREY),
                UIUtils.text(value).fg(Color.BONE)
        );
    }

    private static @NotNull dev.lumenlang.console.element.impl.basic.Text noteLine(@NotNull String text) {
        return UIUtils.text(text).fg(Color.GHOST_GREY);
    }

    private static @NotNull Element candidateLine(int index, @NotNull Suggestion suggestion) {
        return UIUtils.row(
                UIUtils.text("  #" + index + "  ").fg(Color.GHOST_GREY),
                UIUtils.text(String.format("%.3f", suggestion.confidence())).fg(confidenceColor(suggestion.confidence())).bold(),
                UIUtils.text("  "),
                UIUtils.text(suggestion.pattern().raw()).fg(Color.BONE)
        );
    }

    private static @NotNull Element issueLine(@NotNull SuggestionIssue issue) {
        return UIUtils.row(
                UIUtils.text("        " + issueName(issue) + "  ").fg(issueColor(issue)).bold(),
                UIUtils.text(describeIssue(issue)).fg(Color.BONE)
        );
    }

    private static @NotNull Element indent(@NotNull Element child) {
        return UIUtils.row(UIUtils.text("  "), child);
    }

    private static @NotNull Element blank() {
        return UIUtils.text("");
    }

    private static @NotNull Element caseLabel(@NotNull SimulatorRunRecord record, boolean observing) {
        String marker;
        Color color;
        if (observing) {
            marker = "observed";
            color = Color.SOFT_PEACH;
        } else if (record.passed()) {
            marker = "passed";
            color = Color.MINT;
        } else {
            marker = "failed";
            color = Color.ALARM_RED;
        }
        return UIUtils.row(
                UIUtils.text(marker + "  ").fg(color).bold(),
                UIUtils.text(record.caseName()).fg(Color.BONE).bold()
        );
    }

    private static @NotNull String issueName(@NotNull SuggestionIssue issue) {
        if (issue instanceof SuggestionIssue.Typo) return "typo";
        if (issue instanceof SuggestionIssue.ExtraTokens) return "extra tokens";
        if (issue instanceof SuggestionIssue.Reorder) return "reorder";
        if (issue instanceof SuggestionIssue.TypeMismatch) return "type mismatch";
        if (issue instanceof SuggestionIssue.MissingBinding) return "missing binding";
        return issue.getClass().getSimpleName();
    }

    private static @NotNull Color issueColor(@NotNull SuggestionIssue issue) {
        if (issue instanceof SuggestionIssue.Typo) return Color.WARM_YELLOW;
        if (issue instanceof SuggestionIssue.ExtraTokens) return Color.SOFT_PEACH;
        if (issue instanceof SuggestionIssue.Reorder) return Color.MINT;
        if (issue instanceof SuggestionIssue.TypeMismatch) return Color.ALARM_RED;
        if (issue instanceof SuggestionIssue.MissingBinding) return Color.WARM_YELLOW;
        return Color.GHOST_GREY;
    }

    private static @NotNull String describeIssue(@NotNull SuggestionIssue issue) {
        if (issue instanceof SuggestionIssue.Typo t) {
            return "input has '" + t.token().text() + "' at column " + t.token().start() + " where the pattern expects '" + t.expected() + "'";
        }
        if (issue instanceof SuggestionIssue.ExtraTokens e) {
            return "input has trailing tokens the pattern does not consume: " + tokenTexts(e.tokens());
        }
        if (issue instanceof SuggestionIssue.Reorder r) {
            return "tokens appear in the wrong order: " + tokenTexts(r.tokens());
        }
        if (issue instanceof SuggestionIssue.TypeMismatch m) {
            return "'" + m.token().text() + "' could not bind as " + m.bindingId() + " (" + m.reason() + ")";
        }
        if (issue instanceof SuggestionIssue.MissingBinding m) {
            return "the pattern needs a value for binding " + m.bindingId() + " but the input ran out";
        }
        return issue.toString();
    }

    private static @NotNull Color confidenceColor(double confidence) {
        if (confidence >= 0.75) return Color.MINT;
        if (confidence >= 0.5) return Color.WARM_YELLOW;
        return Color.SOFT_PEACH;
    }

    private static @NotNull String tokenTexts(@NotNull List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("'").append(tokens.get(i).text()).append("'");
        }
        return sb.toString();
    }

    @Override
    public void afterAll(@NotNull ExtensionContext context) {
        synchronized (RECORDS) {
            if (RECORDS.isEmpty()) return;
            print(RECORDS);
            RECORDS.clear();
        }
    }
}
