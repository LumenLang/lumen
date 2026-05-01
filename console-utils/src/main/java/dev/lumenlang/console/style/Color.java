package dev.lumenlang.console.style;

import org.jetbrains.annotations.NotNull;

/**
 * 24-bit RGB color. Converted to truecolor ANSI escapes for foreground or background.
 *
 * <p>The named constants on this class are a curated palette tuned to look good on dark
 * terminal backgrounds. They favor slightly desaturated, warmer tones over pure CSS primaries
 * because pure red, pure yellow, and pure green tend to look harsh and cheap in console output.
 */
@SuppressWarnings("unused")
public record Color(int r, int g, int b) {

    public static final Color WARM_YELLOW = new Color(255, 211, 130);
    public static final Color DEEP_AMBER = new Color(255, 168, 76);
    public static final Color SOFT_PEACH = new Color(255, 224, 178);
    public static final Color ALARM_RED = new Color(255, 92, 92);
    public static final Color BLOOD_RED = new Color(220, 38, 38);
    public static final Color GHOST_GREY = new Color(140, 140, 150);
    public static final Color SLATE = new Color(108, 117, 125);
    public static final Color OCEAN = new Color(64, 156, 191);
    public static final Color SKY = new Color(135, 206, 235);
    public static final Color MINT = new Color(168, 230, 207);
    public static final Color FOREST = new Color(54, 138, 92);
    public static final Color LAVENDER = new Color(199, 187, 232);
    public static final Color VIOLET = new Color(155, 89, 182);
    public static final Color CORAL = new Color(255, 127, 102);
    public static final Color ROSE = new Color(232, 152, 178);
    public static final Color CREAM = new Color(245, 230, 200);
    public static final Color DARK_GRAPE = new Color(76, 45, 102);
    public static final Color BONE = new Color(225, 220, 210);
    public static final Color INK = new Color(40, 44, 52);

    /**
     * Returns the ANSI escape that sets the foreground to this color.
     */
    public @NotNull String fg() {
        return Ansi.CSI + "38;2;" + r + ";" + g + ";" + b + "m";
    }

    /**
     * Returns the ANSI escape that sets the background to this color.
     */
    public @NotNull String bg() {
        return Ansi.CSI + "48;2;" + r + ";" + g + ";" + b + "m";
    }

    /**
     * Constructs a color from a CSS-style hex string ({@code "#ff5c5c"} or {@code "ff5c5c"}).
     */
    public static @NotNull Color hex(@NotNull String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        int v = Integer.parseInt(h, 16);
        return new Color((v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF);
    }
}
