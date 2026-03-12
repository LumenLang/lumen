package dev.lumenlang.lumen.pipeline.minicolorize.tag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/**
 * A color tag, either a named color or a hex color.
 *
 * <p>Supports the following syntaxes:
 * <ul>
 *   <li>{@code <yellow>}, {@code <dark_blue>}, etc.</li>
 *   <li>{@code <#RRGGBB>}</li>
 *   <li>{@code <color:yellow>}, {@code <colour:yellow>}, {@code <c:yellow>}</li>
 *   <li>{@code <color:#FF5555>}</li>
 * </ul>
 *
 * @param color the resolved color value, always stored as a lowercase named color or {@code #rrggbb} hex
 */
public record ColorTag(@NotNull String color) implements Tag {

    private static final Map<String, String> NAMED_COLORS = Map.ofEntries(
            Map.entry("black", "black"),
            Map.entry("dark_blue", "dark_blue"),
            Map.entry("dark_green", "dark_green"),
            Map.entry("dark_aqua", "dark_aqua"),
            Map.entry("dark_red", "dark_red"),
            Map.entry("dark_purple", "dark_purple"),
            Map.entry("gold", "gold"),
            Map.entry("gray", "gray"),
            Map.entry("grey", "gray"),
            Map.entry("dark_gray", "dark_gray"),
            Map.entry("dark_grey", "dark_gray"),
            Map.entry("blue", "blue"),
            Map.entry("green", "green"),
            Map.entry("aqua", "aqua"),
            Map.entry("red", "red"),
            Map.entry("light_purple", "light_purple"),
            Map.entry("yellow", "yellow"),
            Map.entry("white", "white")
    );

    /**
     * Attempts to parse a tag name into a ColorTag.
     * Returns null if the name is not a recognized color.
     *
     * @param tagName the tag name (e.g. "yellow", "#FF0000", "color:red")
     * @return the parsed ColorTag, or null
     */
    public static @Nullable ColorTag parse(@NotNull String tagName) {
        String lower = tagName.toLowerCase(Locale.ROOT);

        if (lower.startsWith("#") && isValidHex(lower.substring(1))) {
            return new ColorTag(lower);
        }

        String namedColor = NAMED_COLORS.get(lower);
        if (namedColor != null) {
            return new ColorTag(namedColor);
        }

        String colorArg = stripPrefix(lower);
        if (colorArg != null) {
            if (colorArg.startsWith("#") && isValidHex(colorArg.substring(1))) {
                return new ColorTag(colorArg);
            }
            String resolved = NAMED_COLORS.get(colorArg);
            if (resolved != null) {
                return new ColorTag(resolved);
            }
        }

        return null;
    }

    /**
     * Checks if a named color string is a known Minecraft color constant.
     *
     * @param name the lowercase color name
     * @return true if it is a known named color
     */
    public static boolean isNamedColor(@NotNull String name) {
        return NAMED_COLORS.containsKey(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns a {@link TagResolver} that handles color tags.
     *
     * @return the resolver
     */
    public static @NotNull TagResolver resolver() {
        return (tagName, negated) -> parse(tagName);
    }

    public static boolean isValidHex(@NotNull String hex) {
        if (hex.length() != 6) return false;
        for (char c : hex.toCharArray()) {
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable String stripPrefix(@NotNull String value) {
        for (String prefix : new String[]{"color:", "colour:", "c:"}) {
            if (value.startsWith(prefix)) {
                return value.substring(prefix.length());
            }
        }
        return null;
    }

    @Override
    public @NotNull String describe() {
        return "color(" + color + ")";
    }
}
