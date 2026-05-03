package dev.lumenlang.console.element.util;

import org.jetbrains.annotations.NotNull;

/**
 * Computes the visible width of a string, skipping ANSI CSI escape sequences. The visible width is
 * what an end user sees on screen, which is what layout calculations need.
 */
public final class VisibleLength {

    private VisibleLength() {
    }

    /**
     * Returns the number of visible columns occupied by {@code s} after stripping ANSI CSI escapes.
     */
    public static int of(@NotNull String s) {
        int len = 0;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == 0x1B && i + 1 < s.length() && s.charAt(i + 1) == '[') {
                int end = s.indexOf('m', i);
                if (end < 0) break;
                i = end + 1;
                continue;
            }
            len++;
            i++;
        }
        return len;
    }

    /**
     * Returns {@code s} repeated to the given visible width, padded or clipped as needed.
     */
    public static @NotNull String pad(@NotNull String s, int width) {
        int v = of(s);
        if (v == width) return s;
        if (v < width) return s + " ".repeat(width - v);
        return clip(s, width);
    }

    /**
     * Trims trailing visible whitespace from {@code s}, preserving any ANSI escapes interleaved at
     * the end so the active style is kept and reset where the original wrote it.
     */
    public static @NotNull String stripTrailing(@NotNull String s) {
        int lastVisible = -1;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == 0x1B && i + 1 < s.length() && s.charAt(i + 1) == '[') {
                int end = s.indexOf('m', i);
                if (end < 0) break;
                i = end + 1;
                continue;
            }
            if (c != ' ') lastVisible = i;
            i++;
        }
        if (lastVisible < 0) return "";
        StringBuilder out = new StringBuilder();
        out.append(s, 0, lastVisible + 1);
        int j = lastVisible + 1;
        while (j < s.length()) {
            char c = s.charAt(j);
            if (c == 0x1B && j + 1 < s.length() && s.charAt(j + 1) == '[') {
                int end = s.indexOf('m', j);
                if (end < 0) break;
                out.append(s, j, end + 1);
                j = end + 1;
                continue;
            }
            j++;
        }
        return out.toString();
    }

    /**
     * Clips {@code s} to the given visible width, preserving ANSI escapes.
     */
    public static @NotNull String clip(@NotNull String s, int width) {
        if (width <= 0) return "";
        StringBuilder out = new StringBuilder();
        int visible = 0;
        int i = 0;
        while (i < s.length() && visible < width) {
            char c = s.charAt(i);
            if (c == 0x1B && i + 1 < s.length() && s.charAt(i + 1) == '[') {
                int end = s.indexOf('m', i);
                if (end < 0) break;
                out.append(s, i, end + 1);
                i = end + 1;
                continue;
            }
            out.append(c);
            visible++;
            i++;
        }
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == 0x1B && i + 1 < s.length() && s.charAt(i + 1) == '[') {
                int end = s.indexOf('m', i);
                if (end < 0) break;
                out.append(s, i, end + 1);
                i = end + 1;
                continue;
            }
            break;
        }
        while (visible < width) {
            out.append(' ');
            visible++;
        }
        return out.toString();
    }
}
