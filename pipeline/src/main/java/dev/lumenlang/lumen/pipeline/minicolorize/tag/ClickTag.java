package dev.lumenlang.lumen.pipeline.minicolorize.tag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * A click event tag that triggers an action when the player clicks the text.
 *
 * <p>Supports: {@code <click:action:value>text</click>}
 *
 * <p>Actions:
 * <ul>
 *   <li>{@code open_url}</li>
 *   <li>{@code run_command}</li>
 *   <li>{@code suggest_command}</li>
 *   <li>{@code copy_to_clipboard}</li>
 *   <li>{@code change_page}</li>
 * </ul>
 *
 * @param action the click action type
 * @param value  the action argument
 */
public record ClickTag(@NotNull String action, @NotNull String value) implements Tag {

    /**
     * Attempts to parse a tag name into a ClickTag.
     *
     * @param tagName the tag name (e.g. "click:run_command:/seed")
     * @return the parsed ClickTag, or null
     */
    public static @Nullable ClickTag parse(@NotNull String tagName) {
        String lower = tagName.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("click:")) {
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

        return switch (action) {
            case "open_url", "run_command", "suggest_command", "copy_to_clipboard", "change_page" ->
                    new ClickTag(action, value);
            default -> null;
        };
    }

    /**
     * Returns a {@link TagResolver} that handles click tags.
     *
     * @return the resolver
     */
    public static @NotNull TagResolver resolver() {
        return (tagName, negated) -> parse(tagName);
    }

    @Override
    public @NotNull String describe() {
        return "click(" + action + ", " + value + ")";
    }
}
