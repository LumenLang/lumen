package net.vansencool.lumen.pipeline.minicolorize.tag;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * A reset tag that clears all active formatting.
 *
 * <p>Supports: {@code <reset>} or {@code <r>}
 */
public record ResetTag() implements Tag {

    /**
     * Checks if a tag name matches the reset tag.
     *
     * @param tagName the tag name (e.g. "reset", "r")
     * @return true if this is a reset tag
     */
    public static boolean isReset(@NotNull String tagName) {
        String lower = tagName.toLowerCase(Locale.ROOT);
        return lower.equals("reset") || lower.equals("r");
    }

    /**
     * Returns a {@link TagResolver} that handles reset tags.
     *
     * @return the resolver
     */
    public static @NotNull TagResolver resolver() {
        return (tagName, negated) -> isReset(tagName) ? new ResetTag() : null;
    }

    @Override
    public boolean selfClosing() {
        return true;
    }

    @Override
    public @NotNull String describe() {
        return "reset";
    }
}
