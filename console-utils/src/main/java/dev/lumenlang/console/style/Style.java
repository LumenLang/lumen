package dev.lumenlang.console.style;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable text style: foreground color, background color, and any combination of decoration
 * flags. Apply with {@link #apply(String)} to produce ANSI-wrapped text.
 *
 * <p>Styles compose with {@link #merge(Style)}; later flags win, later non-null colors win.
 */
@SuppressWarnings("unused")
public final class Style {

    public static final Style NONE = new Style(null, null, false, false, false, false, false, false, false);

    private final @Nullable Color fg;
    private final @Nullable Color bg;
    private final boolean bold;
    private final boolean dim;
    private final boolean italic;
    private final boolean underline;
    private final boolean blink;
    private final boolean reverse;
    private final boolean strike;

    private Style(@Nullable Color fg, @Nullable Color bg, boolean bold, boolean dim, boolean italic, boolean underline, boolean blink, boolean reverse, boolean strike) {
        this.fg = fg;
        this.bg = bg;
        this.bold = bold;
        this.dim = dim;
        this.italic = italic;
        this.underline = underline;
        this.blink = blink;
        this.reverse = reverse;
        this.strike = strike;
    }

    /**
     * Shortcut for a style with just a foreground color.
     */
    public static @NotNull Style fg(@NotNull Color color) {
        return NONE.withFg(color);
    }

    /**
     * Shortcut for a style with just a background color.
     */
    public static @NotNull Style bg(@NotNull Color color) {
        return NONE.withBg(color);
    }

    /**
     * Returns a copy with the foreground replaced.
     */
    public @NotNull Style withFg(@Nullable Color color) {
        return new Style(color, bg, bold, dim, italic, underline, blink, reverse, strike);
    }

    /**
     * Returns a copy with the background replaced.
     */
    public @NotNull Style withBg(@Nullable Color color) {
        return new Style(fg, color, bold, dim, italic, underline, blink, reverse, strike);
    }

    /**
     * Returns a copy with the bold flag enabled.
     */
    public @NotNull Style bold() {
        return new Style(fg, bg, true, dim, italic, underline, blink, reverse, strike);
    }

    /**
     * Returns a copy with the dim flag enabled. Many terminals render dim as a muted gray.
     */
    public @NotNull Style dim() {
        return new Style(fg, bg, bold, true, italic, underline, blink, reverse, strike);
    }

    /**
     * Returns a copy with the italic flag enabled. Not all terminals render italic.
     */
    public @NotNull Style italic() {
        return new Style(fg, bg, bold, dim, true, underline, blink, reverse, strike);
    }

    /**
     * Returns a copy with the underline flag enabled.
     */
    public @NotNull Style underline() {
        return new Style(fg, bg, bold, dim, italic, true, blink, reverse, strike);
    }

    /**
     * Returns a copy with the blink flag enabled. Almost no modern terminal honors blink.
     */
    public @NotNull Style blink() {
        return new Style(fg, bg, bold, dim, italic, underline, true, reverse, strike);
    }

    /**
     * Returns a copy with the reverse flag enabled. Swaps foreground and background.
     */
    public @NotNull Style reverse() {
        return new Style(fg, bg, bold, dim, italic, underline, blink, true, strike);
    }

    /**
     * Returns a copy with the strikethrough flag enabled.
     */
    public @NotNull Style strike() {
        return new Style(fg, bg, bold, dim, italic, underline, blink, reverse, true);
    }

    /**
     * Returns a style that layers {@code other} on top of this one. Decoration flags OR together;
     * non-null colors from {@code other} replace this style's colors.
     */
    public @NotNull Style merge(@NotNull Style other) {
        return new Style(
                other.fg != null ? other.fg : fg,
                other.bg != null ? other.bg : bg,
                bold || other.bold,
                dim || other.dim,
                italic || other.italic,
                underline || other.underline,
                blink || other.blink,
                reverse || other.reverse,
                strike || other.strike);
    }

    /**
     * Returns the ANSI prefix that activates this style. Always pair with {@link Ansi#RESET}.
     */
    public @NotNull String prefix() {
        StringBuilder sb = new StringBuilder();
        if (fg != null) sb.append(fg.fg());
        if (bg != null) sb.append(bg.bg());
        if (bold) sb.append(Ansi.BOLD);
        if (dim) sb.append(Ansi.DIM);
        if (italic) sb.append(Ansi.ITALIC);
        if (underline) sb.append(Ansi.UNDERLINE);
        if (blink) sb.append(Ansi.BLINK);
        if (reverse) sb.append(Ansi.REVERSE);
        if (strike) sb.append(Ansi.STRIKE);
        return sb.toString();
    }

    /**
     * Wraps {@code text} in the style's prefix and a trailing {@link Ansi#RESET}. When the style is
     * {@link #NONE}, returns {@code text} unchanged.
     */
    public @NotNull String apply(@NotNull String text) {
        String p = prefix();
        if (p.isEmpty()) return text;
        return p + text + Ansi.RESET;
    }
}
