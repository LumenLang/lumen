package dev.lumenlang.lumen.pipeline.minicolorize.tag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * A hover event tag that shows content when the player hovers over the text.
 *
 * <p>Supports:
 * <ul>
 *   <li>{@code <hover:show_text:'<red>hello'>text</hover>}</li>
 *   <li>{@code <hover:show_item:diamond_sword:1>text</hover>}</li>
 *   <li>{@code <hover:show_entity:minecraft:pig:uuid:name>text</hover>}</li>
 * </ul>
 *
 * @param action the hover action type
 * @param value  the raw value string (interpretation depends on action)
 */
public record HoverTag(@NotNull String action, @NotNull String value) implements Tag {

    /**
     * Attempts to parse a tag name into a HoverTag.
     *
     * @param tagName the tag name (e.g. "hover:show_text:'hello'")
     * @return the parsed HoverTag, or null
     */
    public static @Nullable HoverTag parse(@NotNull String tagName) {
        String lower = tagName.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("hover:")) {
            return null;
        }

        String rest = tagName.substring(6);
        int colon = rest.indexOf(':');
        if (colon < 0) {
            return null;
        }

        String action = rest.substring(0, colon).toLowerCase(Locale.ROOT);
        String value = rest.substring(colon + 1);

        if (value.isEmpty()) {
            return null;
        }

        if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1);
        }

        return switch (action) {
            case "show_text", "show_item", "show_entity" -> new HoverTag(action, value);
            default -> null;
        };
    }

    /**
     * Returns a {@link TagResolver} that handles hover tags.
     *
     * @return the resolver
     */
    public static @NotNull TagResolver resolver() {
        return (tagName, negated) -> parse(tagName);
    }

    @Override
    public @NotNull String describe() {
        return "hover(" + action + ", " + value + ")";
    }
}
