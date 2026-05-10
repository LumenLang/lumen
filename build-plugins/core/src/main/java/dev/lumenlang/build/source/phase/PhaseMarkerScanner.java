package dev.lumenlang.build.source.phase;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Single-pass scanner that finds {@code // lumen:<phase>} marker lines in a
 * source file. Vanta's lexer drops comments, so phase boundaries have to be
 * captured before parsing.
 */
public final class PhaseMarkerScanner {

    private static final Pattern MARKER = Pattern.compile("^\\s*//\\s*lumen:(compile|runtime)\\s*$");

    private PhaseMarkerScanner() {
    }

    /**
     * Returns every marker found in {@code source}, in source order.
     */
    public static @NotNull List<PhaseMarker> scan(@NotNull String source) {
        List<PhaseMarker> out = new ArrayList<>();
        int line = 1;
        int start = 0;
        int len = source.length();
        for (int i = 0; i <= len; i++) {
            if (i == len || source.charAt(i) == '\n') {
                int end = (i > 0 && source.charAt(i - 1) == '\r') ? i - 1 : i;
                Matcher m = MARKER.matcher(source.subSequence(start, end));
                if (m.matches()) {
                    Phase phase = "compile".equals(m.group(1)) ? Phase.COMPILE : Phase.RUNTIME;
                    out.add(new PhaseMarker(phase, line));
                }
                line++;
                start = i + 1;
            }
        }
        return out;
    }
}
