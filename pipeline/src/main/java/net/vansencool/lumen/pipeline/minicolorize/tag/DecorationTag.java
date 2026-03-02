package net.vansencool.lumen.pipeline.minicolorize.tag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * A text decoration tag such as {@code <bold>}, {@code <italic>}, {@code <underlined>}, etc.
 *
 * <p>Supports:
 * <ul>
 *   <li>{@code <bold>} or {@code <b>}</li>
 *   <li>{@code <italic>} or {@code <em>} or {@code <i>}</li>
 *   <li>{@code <underlined>} or {@code <u>}</li>
 *   <li>{@code <strikethrough>} or {@code <st>}</li>
 *   <li>{@code <obfuscated>} or {@code <obf>}</li>
 * </ul>
 *
 * <p>Negation is supported via {@code <!bold>} or {@code <bold:false>}.
 *
 * @param decoration the canonical decoration name
 * @param enabled    true to apply, false to negate
 */
public record DecorationTag(@NotNull String decoration, boolean enabled) implements Tag {

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("bold", "bold"),
            Map.entry("b", "bold"),
            Map.entry("italic", "italic"),
            Map.entry("em", "italic"),
            Map.entry("i", "italic"),
            Map.entry("underlined", "underlined"),
            Map.entry("u", "underlined"),
            Map.entry("strikethrough", "strikethrough"),
            Map.entry("st", "strikethrough"),
            Map.entry("obfuscated", "obfuscated"),
            Map.entry("obf", "obfuscated")
    );

    /**
     * Attempts to parse a tag name into a DecorationTag.
     * Returns null if the name is not a recognized decoration.
     *
     * @param tagName the tag name (e.g. "bold", "b", "italic:false")
     * @param negated true if the tag was prefixed with ! (e.g. {@code <!bold>})
     * @return the parsed DecorationTag, or null
     */
    public static @Nullable DecorationTag parse(@NotNull String tagName, boolean negated) {
        String lower = tagName.toLowerCase(Locale.ROOT);
        boolean enabled = !negated;

        if (lower.endsWith(":false")) {
            enabled = false;
            lower = lower.substring(0, lower.length() - 6);
        } else if (lower.endsWith(":true")) {
            enabled = true;
            lower = lower.substring(0, lower.length() - 5);
        }

        String canonical = ALIASES.get(lower);
        if (canonical != null) {
            return new DecorationTag(canonical, enabled);
        }

        return null;
    }

    /**
     * Returns a {@link TagResolver} that handles decoration tags.
     *
     * @return the resolver
     */
    public static @NotNull TagResolver resolver() {
        return DecorationTag::parse;
    }

    @Override
    public @NotNull String describe() {
        return "decoration(" + decoration + ", " + enabled + ")";
    }
}
