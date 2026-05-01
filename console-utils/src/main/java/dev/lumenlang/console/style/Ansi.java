package dev.lumenlang.console.style;

import org.jetbrains.annotations.NotNull;

/**
 * Raw ANSI escape sequences. Most callers should go through {@link Style} or {@link Color} rather
 * than touching these directly.
 */
public final class Ansi {

    public static final String ESC = "\u001B";
    public static final String CSI = ESC + "[";
    public static final String RESET = CSI + "0m";
    public static final String BOLD = CSI + "1m";
    public static final String DIM = CSI + "2m";
    public static final String ITALIC = CSI + "3m";
    public static final String UNDERLINE = CSI + "4m";
    public static final String BLINK = CSI + "5m";
    public static final String REVERSE = CSI + "7m";
    public static final String STRIKE = CSI + "9m";

    private Ansi() {
    }

    /**
     * Wraps {@code text} between {@code open} and {@link #RESET}.
     */
    public static @NotNull String wrap(@NotNull String open, @NotNull String text) {
        return open + text + RESET;
    }
}
