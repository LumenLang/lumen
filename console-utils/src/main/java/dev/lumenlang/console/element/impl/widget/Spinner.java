package dev.lumenlang.console.element.impl.widget;

import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.style.Color;
import dev.lumenlang.console.style.Style;
import org.jetbrains.annotations.NotNull;

/**
 * Single-glyph animated spinner. Stateless: the caller advances the frame index and constructs a
 * new instance on each tick. Pair with a live renderer to drive frames over time.
 */
public final class Spinner implements Element {

    /**
     * Smooth braille spinner. Eight frames, looks great on truecolor terminals.
     */
    public static final String[] BRAILLE = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};

    /**
     * Classic dot spinner. Wider but readable on any terminal.
     */
    public static final String[] DOTS = {"⣾", "⣽", "⣻", "⢿", "⡿", "⣟", "⣯", "⣷"};

    /**
     * Bouncing line spinner. Plain ASCII, always works.
     */
    public static final String[] LINE = {"-", "\\", "|", "/"};

    /**
     * Circle quarter spinner. Heavy block, good for emphasis.
     */
    public static final String[] CIRCLE = {"◐", "◓", "◑", "◒"};

    private final @NotNull String[] frames;
    private final int frame;
    private final @NotNull Style style;

    private Spinner(@NotNull String[] frames, int frame, @NotNull Style style) {
        this.frames = frames;
        this.frame = frame;
        this.style = style;
    }

    /**
     * Builds a braille spinner at the given frame index, unstyled.
     */
    public static @NotNull Spinner of(int frame) {
        return new Spinner(BRAILLE, frame, Style.fg(Color.MINT));
    }

    /**
     * Builds a spinner with a custom frame set.
     */
    public static @NotNull Spinner of(@NotNull String[] frames, int frame) {
        return new Spinner(frames, frame, Style.fg(Color.MINT));
    }

    /**
     * Returns a copy with the given style applied to the spinner glyph.
     */
    public @NotNull Spinner style(@NotNull Style style) {
        return new Spinner(frames, frame, style);
    }

    @Override
    public int width() {
        return 1;
    }

    @Override
    public int height() {
        return 1;
    }

    @Override
    public @NotNull String @NotNull [] render(int width, int height) {
        String glyph = frames[Math.floorMod(frame, frames.length)];
        String styled = style.apply(glyph);
        StringBuilder sb = new StringBuilder(styled);
        sb.append(" ".repeat(Math.max(0, width - 1)));
        String[] out = new String[Math.max(1, height)];
        out[0] = sb.toString();
        for (int i = 1; i < out.length; i++) out[i] = " ".repeat(width);
        return out;
    }
}
