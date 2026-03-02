package net.vansencool.lumen.pipeline.minicolorize.tag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * A rainbow tag that applies a rainbow color effect across the contained text.
 *
 * <p>Supports:
 * <ul>
 *   <li>{@code <rainbow>text</rainbow>}</li>
 *   <li>{@code <rainbow:!>text</rainbow>} (reversed)</li>
 *   <li>{@code <rainbow:2>text</rainbow>} (phase offset)</li>
 *   <li>{@code <rainbow:!2>text</rainbow>} (reversed with phase)</li>
 * </ul>
 *
 * @param reversed whether the rainbow direction is reversed
 * @param phase    the phase offset (default 0)
 */
public record RainbowTag(boolean reversed, float phase) implements Tag {

    /**
     * Attempts to parse a tag name into a RainbowTag.
     *
     * @param tagName the tag name (e.g. "rainbow", "rainbow:!", "rainbow:2", "rainbow:!2")
     * @return the parsed RainbowTag, or null
     */
    public static @Nullable RainbowTag parse(@NotNull String tagName) {
        String lower = tagName.toLowerCase(Locale.ROOT);
        if (!lower.equals("rainbow") && !lower.startsWith("rainbow:")) {
            return null;
        }

        if (lower.equals("rainbow")) {
            return new RainbowTag(false, 0);
        }

        String arg = lower.substring(8);
        if (arg.isEmpty()) {
            return new RainbowTag(false, 0);
        }

        boolean reversed = false;
        if (arg.startsWith("!")) {
            reversed = true;
            arg = arg.substring(1);
        }

        float phase = 0;
        if (!arg.isEmpty()) {
            try {
                phase = Float.parseFloat(arg);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return new RainbowTag(reversed, phase);
    }

    /**
     * Returns a {@link TagResolver} that handles rainbow tags.
     *
     * @return the resolver
     */
    public static @NotNull TagResolver resolver() {
        return (tagName, negated) -> parse(tagName);
    }

    @Override
    public @NotNull String describe() {
        return "rainbow(reversed=" + reversed + ", phase=" + phase + ")";
    }
}
