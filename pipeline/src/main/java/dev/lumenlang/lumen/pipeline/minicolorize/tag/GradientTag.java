package dev.lumenlang.lumen.pipeline.minicolorize.tag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A gradient tag that applies a smooth color gradient across the contained text.
 *
 * <p>Supports:
 * <ul>
 *   <li>{@code <gradient>text</gradient>} (default white to black)</li>
 *   <li>{@code <gradient:#5e4fa2:#f79459>text</gradient>}</li>
 *   <li>{@code <gradient:green:blue>text</gradient>}</li>
 *   <li>{@code <gradient:#5e4fa2:#f79459:red>text</gradient>} (multi-stop)</li>
 * </ul>
 *
 * <p>The last argument may be a float phase value (range -1 to 1) to shift the gradient.
 *
 * @param colors the gradient color stops (named or hex)
 * @param phase  the phase offset (default 0)
 */
public record GradientTag(@NotNull List<String> colors, float phase) implements Tag {

    /**
     * Attempts to parse a tag name into a GradientTag.
     *
     * @param tagName the tag name (e.g. "gradient:#5e4fa2:#f79459")
     * @return the parsed GradientTag, or null
     */
    public static @Nullable GradientTag parse(@NotNull String tagName) {
        String lower = tagName.toLowerCase(Locale.ROOT);
        if (!lower.equals("gradient") && !lower.startsWith("gradient:")) {
            return null;
        }

        if (lower.equals("gradient")) {
            return new GradientTag(List.of("white", "black"), 0);
        }

        String rest = tagName.substring(9);
        if (rest.isEmpty()) {
            return new GradientTag(List.of("white", "black"), 0);
        }

        String[] parts = rest.split(":");
        List<String> colors = new ArrayList<>();
        float phase = 0;

        for (String part : parts) {
            String partLower = part.toLowerCase(Locale.ROOT);
            if (isColor(partLower)) {
                colors.add(partLower);
            } else {
                try {
                    phase = Float.parseFloat(part);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        if (colors.isEmpty()) {
            colors.add("white");
            colors.add("black");
        } else if (colors.size() == 1) {
            colors.add("white");
        }

        return new GradientTag(Collections.unmodifiableList(colors), phase);
    }

    /**
     * Returns a {@link TagResolver} that handles gradient tags.
     *
     * @return the resolver
     */
    public static @NotNull TagResolver resolver() {
        return (tagName, negated) -> parse(tagName);
    }

    private static boolean isColor(@NotNull String value) {
        if (value.startsWith("#") && value.length() == 7) {
            return ColorTag.isValidHex(value.substring(1));
        }
        return ColorTag.isNamedColor(value);
    }

    @Override
    public @NotNull String describe() {
        return "gradient(" + colors + ", phase=" + phase + ")";
    }
}
