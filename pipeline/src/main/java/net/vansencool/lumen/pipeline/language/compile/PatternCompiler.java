package net.vansencool.lumen.pipeline.language.compile;

import net.vansencool.lumen.pipeline.language.pattern.Pattern;
import net.vansencool.lumen.pipeline.language.pattern.PatternPart;
import net.vansencool.lumen.pipeline.language.pattern.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Compiles pattern strings into structured {@link Pattern} objects.
 *
 * <p>
 * Pattern syntax:
 * <ul>
 * <li>{@code %name:TYPE%} A placeholder with an explicit type</li>
 * <li>{@code %name%} A placeholder with the implicit EXPR type</li>
 * <li>{@code literal} Fixed text (case-insensitive match)</li>
 * <li>{@code (a|b|c)} Required choice: one alternative must match</li>
 * <li>{@code [text]} Optional: text may be present or absent</li>
 * <li>{@code [a|b|c]} Optional choice: zero or one alternative matches</li>
 * <li>{@code word[suffix]} Optional suffix, e.g. {@code info[rmation]}</li>
 * <li>{@code [prefix]word} Optional prefix, e.g. {@code [un]hide}</li>
 * </ul>
 *
 * @see Pattern
 */
public final class PatternCompiler {

    private PatternCompiler() {
    }

    /**
     * Compiles a pattern string into a structured Pattern object.
     *
     * @param raw the pattern string to compile
     * @return a compiled Pattern containing the parsed parts
     */
    public static @NotNull Pattern compile(@NotNull String raw) {
        return new Pattern(raw, compileParts(raw.trim()));
    }

    private static @NotNull List<PatternPart> compileParts(@NotNull String raw) {
        List<PatternPart> parts = new ArrayList<>();
        int i = 0;
        int len = raw.length();

        while (i < len) {
            while (i < len && raw.charAt(i) == ' ')
                i++;
            if (i >= len)
                break;

            char c = raw.charAt(i);

            if (c == '%') {
                int j = raw.indexOf('%', i + 1);
                if (j < 0)
                    throw new IllegalArgumentException("Unclosed placeholders '%' in pattern: " + raw);
                String inner = raw.substring(i + 1, j);
                i = j + 1;

                int colon = inner.indexOf(':');
                String name = colon < 0 ? inner : inner.substring(0, colon);
                String type = colon < 0 ? "EXPR" : inner.substring(colon + 1);
                parts.add(new PatternPart.PlaceholderPart(new Placeholder(name, type)));

                if (i < len && raw.charAt(i) != ' ' && raw.charAt(i) != '%'
                        && raw.charAt(i) != '(' && raw.charAt(i) != ')') {
                    int wordEnd = readWordEnd(raw, i);
                    String suffix = raw.substring(i, wordEnd);
                    i = wordEnd;
                    parts.add(toWordPart(suffix));
                }

            } else if (c == '(') {
                int j = findClosing(raw, i, '(', ')');
                String inner = raw.substring(i + 1, j);
                i = j + 1;
                List<List<PatternPart>> alternatives = parseAlternatives(inner);
                if (i < raw.length() && raw.charAt(i) == '[') {
                    int k = findClosing(raw, i, '[', ']');
                    String suffix = raw.substring(i + 1, k);
                    i = k + 1;
                    alternatives = applyFlexSuffix(alternatives, suffix);
                }
                parts.add(new PatternPart.Group(alternatives, true));

            } else if (c == '[') {
                int j = findClosing(raw, i, '[', ']');
                String inner = raw.substring(i + 1, j);
                i = j + 1;

                if (i < len && isWordChar(raw.charAt(i))) {
                    int wordEnd = readWordEnd(raw, i);
                    String rest = raw.substring(i, wordEnd);
                    i = wordEnd;
                    List<String> restForms = expandBrackets(rest);
                    List<String> allForms = new ArrayList<>();
                    for (String rf : restForms) {
                        allForms.add(rf.toLowerCase(Locale.ROOT));
                        allForms.add((inner + rf).toLowerCase(Locale.ROOT));
                    }
                    parts.add(new PatternPart.FlexLiteral(allForms));
                } else {
                    parts.add(new PatternPart.Group(parseAlternatives(inner), false));
                }

            } else {
                int wordEnd = readWordEnd(raw, i);
                String word = raw.substring(i, wordEnd);
                i = wordEnd;
                parts.add(toWordPart(word));
            }
        }

        return parts;
    }

    private static int findClosing(@NotNull String raw, int start, char open, char close) {
        int depth = 0;
        for (int i = start; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == open)
                depth++;
            else if (c == close) {
                depth--;
                if (depth == 0)
                    return i;
            }
        }
        throw new IllegalArgumentException("Unclosed '" + open + "' in pattern: " + raw);
    }

    private static boolean isWordChar(char c) {
        return c != ' ' && c != '%' && c != '(' && c != ')';
    }

    private static int readWordEnd(@NotNull String raw, int i) {
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == ' ' || c == '%' || c == '(')
                break;
            if (c == '[') {
                i = findClosing(raw, i, '[', ']') + 1;
                continue;
            }
            i++;
        }
        return i;
    }

    private static @NotNull List<String> expandBrackets(@NotNull String word) {
        int open = word.indexOf('[');
        if (open < 0)
            return List.of(word);
        int close = findClosing(word, open, '[', ']');
        String before = word.substring(0, open);
        String optional = word.substring(open + 1, close);
        String after = word.substring(close + 1);
        List<String> afterForms = expandBrackets(after);
        List<String> result = new ArrayList<>();
        for (String af : afterForms) {
            result.add(before + af);
            result.add(before + optional + af);
        }
        return result;
    }

    private static @NotNull List<List<PatternPart>> parseAlternatives(@NotNull String inner) {
        List<String> alts = splitByPipe(inner);
        List<List<PatternPart>> result = new ArrayList<>();
        for (String alt : alts) {
            result.add(compileParts(alt.trim()));
        }
        return result;
    }

    /**
     * Applies a flex bracket suffix to each alternative in a required group.
     *
     * <p>When a pattern contains {@code (tick|second|minute)[s]}, this method transforms
     * the last part of each alternative into a {@link PatternPart.FlexLiteral} that accepts
     * both the base form and the suffixed form (e.g. "tick" and "ticks").
     *
     * @param alternatives the parsed alternatives from the group
     * @param suffix       the text inside the brackets (e.g. "s")
     * @return a new list of alternatives with the suffix applied to each last literal
     */
    private static @NotNull List<List<PatternPart>> applyFlexSuffix(
            @NotNull List<List<PatternPart>> alternatives, @NotNull String suffix) {
        List<List<PatternPart>> result = new ArrayList<>(alternatives.size());
        for (List<PatternPart> alt : alternatives) {
            if (alt.isEmpty()) {
                result.add(alt);
                continue;
            }
            PatternPart last = alt.get(alt.size() - 1);
            if (last instanceof PatternPart.Literal lit) {
                List<PatternPart> modified = new ArrayList<>(alt);
                modified.set(modified.size() - 1, new PatternPart.FlexLiteral(
                        List.of(lit.text(), lit.text() + suffix.toLowerCase(Locale.ROOT))));
                result.add(modified);
            } else if (last instanceof PatternPart.FlexLiteral flex) {
                List<String> expanded = new ArrayList<>();
                for (String form : flex.forms()) {
                    expanded.add(form);
                    expanded.add(form + suffix.toLowerCase(Locale.ROOT));
                }
                List<PatternPart> modified = new ArrayList<>(alt);
                modified.set(modified.size() - 1, new PatternPart.FlexLiteral(expanded));
                result.add(modified);
            } else {
                result.add(alt);
            }
        }
        return result;
    }

    private static @NotNull List<String> splitByPipe(@NotNull String s) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[' || c == '(')
                depth++;
            else if (c == ']' || c == ')')
                depth--;
            else if (c == '|' && depth == 0) {
                result.add(s.substring(start, i));
                start = i + 1;
            }
        }
        result.add(s.substring(start));
        return result;
    }

    private static @NotNull PatternPart toWordPart(@NotNull String word) {
        if (word.indexOf('[') >= 0) {
            List<String> forms = expandBrackets(word);
            return new PatternPart.FlexLiteral(
                    forms.stream().map(f -> f.toLowerCase(Locale.ROOT)).toList());
        }
        return new PatternPart.Literal(word.toLowerCase(Locale.ROOT));
    }
}
