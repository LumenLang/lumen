package dev.lumenlang.lumen.headless.sim.snapshot;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes {@link Snapshot} as deterministic plain text.
 */
public final class SnapshotSerializer {

    private SnapshotSerializer() {
    }

    /**
     * Renders {@code snap} to a string ready to be written to disk.
     */
    public static @NotNull String serialize(@NotNull Snapshot snap) {
        StringBuilder sb = new StringBuilder();
        sb.append("case: ").append(snap.caseName()).append('\n');
        sb.append("input: ").append(snap.input()).append('\n');
        sb.append("runner: ").append(snap.runner()).append('\n');
        if (snap.env().isEmpty()) {
            sb.append("env: <empty>\n");
        } else {
            sb.append("env:\n");
            for (String line : snap.env()) sb.append("  ").append(line).append('\n');
        }
        sb.append("suggestions: ").append(snap.suggestions().size()).append('\n');
        for (int i = 0; i < snap.suggestions().size(); i++) {
            SuggestionSnap s = snap.suggestions().get(i);
            sb.append("  [#").append(i).append("] ").append(s.patternRaw()).append('\n');
            sb.append("    confidence: ").append(formatConfidence(s.confidence())).append('\n');
            if (s.issues().isEmpty()) {
                sb.append("    issues: <none>\n");
            } else {
                sb.append("    issues:\n");
                for (String issue : s.issues()) sb.append("      - ").append(issue).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Parses {@code text} produced by {@link #serialize}.
     */
    public static @NotNull Snapshot deserialize(@NotNull String text) {
        String[] lines = text.split("\n", -1);
        int i = 0;
        String caseName = readKv(lines[i++], "case");
        String input = readKv(lines[i++], "input");
        String runner = readKv(lines[i++], "runner");
        List<String> env = new ArrayList<>();
        if (lines[i].equals("env: <empty>")) {
            i++;
        } else if (lines[i].equals("env:")) {
            i++;
            while (i < lines.length && lines[i].startsWith("  ") && !lines[i].startsWith("  [") && !lines[i].startsWith("suggestions:")) {
                env.add(lines[i].substring(2));
                i++;
            }
        }
        int count = Integer.parseInt(readKv(lines[i++], "suggestions"));
        List<SuggestionSnap> suggestions = new ArrayList<>(count);
        for (int s = 0; s < count; s++) {
            String header = lines[i++];
            String patternRaw = header.substring(header.indexOf(']') + 2);
            double confidence = Double.parseDouble(readKv(lines[i++].strip(), "confidence"));
            List<String> issues = new ArrayList<>();
            String head = lines[i++].strip();
            if (head.equals("issues:")) {
                while (i < lines.length && lines[i].startsWith("      - ")) {
                    issues.add(lines[i].substring(8));
                    i++;
                }
            }
            suggestions.add(new SuggestionSnap(patternRaw, confidence, issues));
        }
        return new Snapshot(caseName, input, runner, env, suggestions);
    }

    private static @NotNull String formatConfidence(double v) {
        return String.format("%.3f", v);
    }

    private static @NotNull String readKv(@NotNull String line, @NotNull String key) {
        String prefix = key + ": ";
        if (!line.startsWith(prefix)) {
            throw new IllegalArgumentException("expected '" + prefix + "...' but got: " + line);
        }
        return line.substring(prefix.length());
    }
}
