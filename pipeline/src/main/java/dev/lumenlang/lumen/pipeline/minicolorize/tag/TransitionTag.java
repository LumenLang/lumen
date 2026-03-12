package dev.lumenlang.lumen.pipeline.minicolorize.tag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A transition tag that transitions between colors. Similar to a gradient, but
 * everything is the same color and the phase chooses that color.
 *
 * <p>Supports:
 * <ul>
 *   <li>{@code <transition:#00ff00:#ff0000:0>text</transition>}</li>
 *   <li>{@code <transition:white:black:red:0.5>text</transition>}</li>
 * </ul>
 *
 * @param colors the transition color stops (named or hex)
 * @param phase  the phase value (range -1 to 1) that selects the current color
 */
public record TransitionTag(@NotNull List<String> colors, float phase) implements Tag {

    /**
     * Attempts to parse a tag name into a TransitionTag.
     *
     * @param tagName the tag name (e.g. "transition:#00ff00:#ff0000:0")
     * @return the parsed TransitionTag, or null
     */
    public static @Nullable TransitionTag parse(@NotNull String tagName) {
        String lower = tagName.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("transition:")) {
            return null;
        }

        String rest = tagName.substring(11);
        if (rest.isEmpty()) {
            return null;
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

        if (colors.size() < 2) {
            return null;
        }

        return new TransitionTag(Collections.unmodifiableList(colors), phase);
    }

    /**
     * Returns a {@link TagResolver} that handles transition tags.
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
        return "transition(" + colors + ", phase=" + phase + ")";
    }
}
