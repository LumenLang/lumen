package dev.lumenlang.lumen.headless.sim.widgets;

import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.element.util.VisibleLength;
import dev.lumenlang.console.style.Color;
import dev.lumenlang.console.style.Style;
import org.jetbrains.annotations.NotNull;

/**
 * Ten-segment confidence meter rendered as filled and empty cells. Segment color follows
 * the tier the confidence falls into.
 */
public final class ConfidenceMeter implements Element {

    private static final int SEGMENTS = 10;
    private static final String FILL = "█";
    private static final String EMPTY = "·";

    private final double confidence;

    private ConfidenceMeter(double confidence) {
        this.confidence = confidence;
    }

    /**
     * Meter for a confidence value in {@code [0, 1]}.
     */
    public static @NotNull ConfidenceMeter of(double confidence) {
        return new ConfidenceMeter(confidence);
    }

    private static @NotNull Color colorFor(double value) {
        if (value >= 0.75) return Color.MINT;
        if (value >= 0.5) return Color.WARM_YELLOW;
        if (value >= 0.25) return Color.DEEP_AMBER;
        return Color.CORAL;
    }

    @Override
    public int width() {
        return SEGMENTS;
    }

    @Override
    public int height() {
        return 1;
    }

    @Override
    public @NotNull String @NotNull [] render(int width, int height) {
        double clamped = Math.max(0, Math.min(1, confidence));
        int filled = (int) Math.round(clamped * SEGMENTS);
        Style fillStyle = Style.fg(colorFor(clamped));
        Style emptyStyle = Style.fg(Color.SLATE).dim();
        StringBuilder sb = new StringBuilder();
        if (filled > 0) sb.append(fillStyle.apply(FILL.repeat(filled)));
        int empty = SEGMENTS - filled;
        if (empty > 0) sb.append(emptyStyle.apply(EMPTY.repeat(empty)));
        String[] out = new String[Math.max(1, height)];
        out[0] = VisibleLength.pad(sb.toString(), width);
        for (int i = 1; i < out.length; i++) out[i] = " ".repeat(width);
        return out;
    }
}
