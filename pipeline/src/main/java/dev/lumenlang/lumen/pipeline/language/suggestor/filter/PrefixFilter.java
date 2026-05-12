package dev.lumenlang.lumen.pipeline.language.suggestor.filter;

import dev.lumenlang.lumen.api.language.Suggestion;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Case-insensitive {@code startsWith} filter over {@link Suggestion#insertText()}. Empty prefix
 * passes everything through.
 */
public final class PrefixFilter {

    private PrefixFilter() {
    }

    /**
     * Returns the subset of {@code candidates} whose insert text begins with {@code prefix}
     * (case-insensitive). When {@code prefix} is empty, returns the input unchanged.
     */
    public static @NotNull List<Suggestion> apply(@NotNull List<Suggestion> candidates, @NotNull String prefix) {
        if (prefix.isEmpty()) return candidates;
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<Suggestion> out = new ArrayList<>(candidates.size());
        for (Suggestion s : candidates) {
            if (s.insertText().toLowerCase(Locale.ROOT).startsWith(lower)) out.add(s);
        }
        return List.copyOf(out);
    }

    /**
     * Returns {@code true} when {@code value} starts with {@code prefix} (case-insensitive).
     */
    public static boolean matches(@NotNull String value, @NotNull String prefix) {
        if (prefix.isEmpty()) return true;
        return value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }
}
