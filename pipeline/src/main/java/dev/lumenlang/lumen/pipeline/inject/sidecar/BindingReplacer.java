package dev.lumenlang.lumen.pipeline.inject.sidecar;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitutes placeholder names with their per-script Java expressions in a
 * line of preserved handler source, leaving string and char literals
 * untouched.
 */
public final class BindingReplacer {

    private BindingReplacer() {
    }

    public static @NotNull String replace(@NotNull String line, @NotNull Map<String, String> bindings) {
        String result = line;
        for (Map.Entry<String, String> entry : bindings.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (name.equals(value)) continue;
            result = replaceOutsideLiterals(result, name, value);
        }
        return result;
    }

    private static @NotNull String replaceOutsideLiterals(@NotNull String line, @NotNull String target, @NotNull String replacement) {
        Pattern p = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'|\\b" + Pattern.quote(target) + "\\b");
        Matcher m = p.matcher(line);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String token = m.group();
            if (token.startsWith("\"") || token.startsWith("'")) {
                m.appendReplacement(sb, Matcher.quoteReplacement(token));
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
