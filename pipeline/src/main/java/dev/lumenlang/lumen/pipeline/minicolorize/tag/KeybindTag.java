package dev.lumenlang.lumen.pipeline.minicolorize.tag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * A keybind tag that displays the player's configured key for an action.
 *
 * <p>Supports: {@code <key:key.jump>}
 *
 * @param key the keybind identifier (e.g. "key.jump")
 */
public record KeybindTag(@NotNull String key) implements Tag {

    /**
     * Attempts to parse a tag name into a KeybindTag.
     *
     * @param tagName the tag name (e.g. "key:key.jump")
     * @return the parsed KeybindTag, or null
     */
    public static @Nullable KeybindTag parse(@NotNull String tagName) {
        String lower = tagName.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("key:")) {
            return null;
        }

        String key = tagName.substring(4);
        if (key.isEmpty()) {
            return null;
        }

        return new KeybindTag(key);
    }

    /**
     * Returns a {@link TagResolver} that handles keybind tags.
     *
     * @return the resolver
     */
    public static @NotNull TagResolver resolver() {
        return (tagName, negated) -> parse(tagName);
    }

    @Override
    public boolean selfClosing() {
        return true;
    }

    @Override
    public @NotNull String describe() {
        return "keybind(" + key + ")";
    }
}
