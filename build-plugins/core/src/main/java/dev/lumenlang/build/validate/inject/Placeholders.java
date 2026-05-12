package dev.lumenlang.build.validate.inject;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts placeholder definitions from a Lumen pattern string. A placeholder
 * has the form {@code %name:BINDING%}; this class returns a map from name to
 * binding id.
 */
public final class Placeholders {

    private static final Pattern REGEX = Pattern.compile("%([A-Za-z_][A-Za-z_0-9]*):([A-Za-z_][A-Za-z_0-9]*)%");

    private Placeholders() {
    }

    public static @NotNull Map<String, String> bindings(@NotNull String pattern) {
        Map<String, String> result = new HashMap<>();
        Matcher m = REGEX.matcher(pattern);
        while (m.find()) result.put(m.group(1), m.group(2));
        return result;
    }
}
