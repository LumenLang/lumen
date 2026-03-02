package net.vansencool.lumen.pipeline.minicolorize.tag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * An insertion tag that inserts text into the player's chat input on shift-click.
 *
 * <p>Supports: {@code <insert:text>content</insert>}
 *
 * @param text the text to insert into chat
 */
public record InsertionTag(@NotNull String text) implements Tag {

    /**
     * Attempts to parse a tag name into an InsertionTag.
     *
     * @param tagName the tag name (e.g. "insert:hello world")
     * @return the parsed InsertionTag, or null
     */
    public static @Nullable InsertionTag parse(@NotNull String tagName) {
        String lower = tagName.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("insert:") && !lower.startsWith("insertion:")) {
            return null;
        }

        int colon = tagName.indexOf(':');
        String text = tagName.substring(colon + 1);
        if (text.isEmpty()) {
            return null;
        }

        return new InsertionTag(text);
    }

    /**
     * Returns a {@link TagResolver} that handles insertion tags.
     *
     * @return the resolver
     */
    public static @NotNull TagResolver resolver() {
        return (tagName, negated) -> parse(tagName);
    }

    @Override
    public @NotNull String describe() {
        return "insertion(" + text + ")";
    }
}
