package dev.lumenlang.lumen.pipeline.minicolorize.tag;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A translatable tag that displays a localized message using the player's locale.
 *
 * <p>Supports:
 * <ul>
 *   <li>{@code <lang:block.minecraft.diamond_block>}</li>
 *   <li>{@code <lang:commands.drop.success.single:'<red>1':'<blue>Stone'>}</li>
 *   <li>{@code <tr:key>}, {@code <translate:key>} as aliases</li>
 * </ul>
 *
 * @param key      the translation key
 * @param fallback the fallback text (empty string if none)
 * @param args     the optional placeholder arguments
 */
public record TranslatableTag(@NotNull String key, @NotNull String fallback,
                              @NotNull List<String> args) implements Tag {

    /**
     * Attempts to parse a tag name into a TranslatableTag.
     *
     * @param tagName the tag name (e.g. "lang:block.minecraft.stone")
     * @return the parsed TranslatableTag, or null
     */
    public static @Nullable TranslatableTag parse(@NotNull String tagName) {
        String lower = tagName.toLowerCase(Locale.ROOT);

        String rest;
        boolean hasFallback = false;

        if (lower.startsWith("lang_or:") || lower.startsWith("tr_or:") || lower.startsWith("translate_or:")) {
            int colon = tagName.indexOf(':');
            rest = tagName.substring(colon + 1);
            hasFallback = true;
        } else if (lower.startsWith("lang:") || lower.startsWith("tr:") || lower.startsWith("translate:")) {
            int colon = tagName.indexOf(':');
            rest = tagName.substring(colon + 1);
        } else {
            return null;
        }

        if (rest.isEmpty()) {
            return null;
        }

        List<String> parts = splitQuotedArgs(rest);
        if (parts.isEmpty()) {
            return null;
        }

        String key = parts.get(0);
        String fallback = "";
        List<String> arguments = new ArrayList<>();

        if (hasFallback) {
            if (parts.size() >= 2) {
                fallback = unquote(parts.get(1));
            }
            for (int i = 2; i < parts.size(); i++) {
                arguments.add(unquote(parts.get(i)));
            }
        } else {
            for (int i = 1; i < parts.size(); i++) {
                arguments.add(unquote(parts.get(i)));
            }
        }

        return new TranslatableTag(key, fallback, Collections.unmodifiableList(arguments));
    }

    /**
     * Returns a {@link TagResolver} that handles translatable and fallback tags.
     *
     * @return the resolver
     */
    public static @NotNull TagResolver resolver() {
        return (tagName, negated) -> parse(tagName);
    }

    private static @NotNull List<String> splitQuotedArgs(@NotNull String input) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        int depth = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '\'' && depth == 0) {
                inQuote = !inQuote;
                current.append(c);
                continue;
            }

            if (c == '<') depth++;
            if (c == '>') depth--;

            if (c == ':' && !inQuote && depth == 0) {
                result.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(c);
        }

        if (!current.isEmpty()) {
            result.add(current.toString());
        }

        return result;
    }

    private static @NotNull String unquote(@NotNull String value) {
        if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    @Override
    public @NotNull String describe() {
        StringBuilder sb = new StringBuilder("translatable(" + key);
        if (!fallback.isEmpty()) {
            sb.append(", fallback=").append(fallback);
        }
        if (!args.isEmpty()) {
            sb.append(", args=").append(args);
        }
        sb.append(")");
        return sb.toString();
    }
}
