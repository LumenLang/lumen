package dev.lumenlang.lumen.pipeline.language.tokenization;

import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reports indentation problems.
 */
public final class IndentDiagnostics {

    private static final int TAB_WIDTH = 4;

    private IndentDiagnostics() {
    }

    /**
     * Analyzes raw script source for indentation issues.
     */
    public static @NotNull List<LumenDiagnostic> analyze(@NotNull String src) {
        String[] split = src.split("\\R", -1);
        List<LineIndent> indents = new ArrayList<>();
        List<LumenDiagnostic> diagnostics = new ArrayList<>();

        for (int i = 0; i < split.length; i++) {
            String raw = split[i];
            int p = 0;
            int width = 0;
            int firstTabAfterSpace = -1;
            int firstSpaceAfterTab = -1;
            boolean sawTab = false;
            boolean sawSpace = false;
            while (p < raw.length()) {
                char c = raw.charAt(p);
                if (c == ' ') {
                    width++;
                    if (sawTab && firstSpaceAfterTab == -1) firstSpaceAfterTab = p;
                    sawSpace = true;
                } else if (c == '\t') {
                    width += TAB_WIDTH;
                    if (sawSpace && firstTabAfterSpace == -1) firstTabAfterSpace = p;
                    sawTab = true;
                } else break;
                p++;
            }

            String content = raw.substring(p);
            String trimmed = content.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith("#") || trimmed.startsWith("//")) continue;

            int lineNumber = i + 1;
            indents.add(new LineIndent(lineNumber, raw, width, p, sawTab, sawSpace));

            if (sawTab && sawSpace) {
                int badStart = firstTabAfterSpace >= 0 ? firstTabAfterSpace : firstSpaceAfterTab;
                diagnostics.add(LumenDiagnostic.warning("mixed tabs and spaces in indentation")
                        .at(lineNumber, raw)
                        .highlight(badStart, badStart + 1)
                        .label(firstTabAfterSpace >= 0 ? "tab after spaces" : "space after tabs")
                        .note("tabs count as " + TAB_WIDTH + " spaces, mixing makes nesting depth ambiguous")
                        .help("pick one and use it consistently across the script")
                        .build());
            }
        }

        Set<Integer> flaggedLines = new HashSet<>();

        int pureTabLines = 0;
        int pureSpaceLines = 0;
        for (LineIndent li : indents) {
            if (li.width == 0) continue;
            if (li.usesTab && !li.usesSpace) pureTabLines++;
            else if (li.usesSpace && !li.usesTab) pureSpaceLines++;
        }
        if (pureTabLines > 0 && pureSpaceLines > 0) {
            boolean tabDominant = pureTabLines >= pureSpaceLines;
            String dominantKind = tabDominant ? "tabs" : "spaces";
            String minorityKind = tabDominant ? "spaces" : "tabs";
            for (LineIndent li : indents) {
                if (li.width == 0) continue;
                boolean isMinority = tabDominant ? (li.usesSpace && !li.usesTab) : (li.usesTab && !li.usesSpace);
                if (!isMinority) continue;
                flaggedLines.add(li.lineNumber);
                diagnostics.add(LumenDiagnostic.warning("indent character does not match script")
                        .at(li.lineNumber, li.raw)
                        .highlight(0, li.indentEnd)
                        .label("indented with " + minorityKind)
                        .note("rest of the script uses " + dominantKind + " (" + (tabDominant ? pureTabLines : pureSpaceLines) + " lines vs " + (tabDominant ? pureSpaceLines : pureTabLines) + ")")
                        .help("re-indent this line with " + dominantKind)
                        .build());
            }
        }

        if (indents.size() < 2) return diagnostics;

        Map<Integer, Integer> stepCounts = new HashMap<>();
        for (int i = 1; i < indents.size(); i++) {
            int diff = indents.get(i).width - indents.get(i - 1).width;
            if (diff > 0) stepCounts.merge(diff, 1, Integer::sum);
        }
        if (stepCounts.isEmpty()) return diagnostics;

        int dominant = stepCounts.entrySet().stream()
                .max(Map.Entry.<Integer, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .get()
                .getKey();

        if (stepCounts.size() > 1) {
            for (int i = 1; i < indents.size(); i++) {
                int diff = indents.get(i).width - indents.get(i - 1).width;
                if (diff > 0 && diff != dominant) {
                    LineIndent li = indents.get(i);
                    if (flaggedLines.contains(li.lineNumber)) continue;
                    flaggedLines.add(li.lineNumber);
                    diagnostics.add(LumenDiagnostic.warning("inconsistent indent step")
                            .at(li.lineNumber, li.raw)
                            .highlight(0, li.indentEnd)
                            .label("step of " + diff + ", expected " + dominant)
                            .note("dominant step in this script is " + dominant + " " + spaceWord(dominant))
                            .note("found steps: " + formatStepCounts(stepCounts))
                            .help("normalize every nesting level to a step of " + dominant + " " + spaceWord(dominant))
                            .build());
                }
            }
        }

        for (LineIndent li : indents) {
            if (flaggedLines.contains(li.lineNumber)) continue;
            if (li.width > 0 && li.width % dominant != 0) {
                int floorMul = (li.width / dominant) * dominant;
                int ceilMul = floorMul + dominant;
                int suggested = (li.width - floorMul) <= (ceilMul - li.width) ? floorMul : ceilMul;
                if (suggested == 0) suggested = dominant;
                diagnostics.add(LumenDiagnostic.warning("indent not aligned to step")
                        .at(li.lineNumber, li.raw)
                        .highlight(0, li.indentEnd)
                        .label(li.width + " " + spaceWord(li.width) + " is not a multiple of " + dominant)
                        .note("expected step is " + dominant + " " + spaceWord(dominant))
                        .help("use " + suggested + " " + spaceWord(suggested) + " at this level")
                        .build());
            }
        }

        return diagnostics;
    }

    private static @NotNull String spaceWord(int n) {
        return n == 1 ? "space" : "spaces";
    }

    private static @NotNull String formatStepCounts(@NotNull Map<Integer, Integer> stepCounts) {
        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(stepCounts.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append(", ");
            Map.Entry<Integer, Integer> e = entries.get(i);
            sb.append(e.getKey()).append(" ").append(spaceWord(e.getKey())).append(" x").append(e.getValue());
        }
        return sb.toString();
    }

    private record LineIndent(int lineNumber, @NotNull String raw, int width, int indentEnd, boolean usesTab, boolean usesSpace) {
    }
}
