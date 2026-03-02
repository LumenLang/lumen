package net.vansencool.lumen.pipeline.language.tokenization;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits raw Lumen script source text into a list of {@link Line} objects, each containing
 * the tokenized form of one non-blank, non-comment source line.
 *
 * <h2>Tokenization Rules</h2>
 * <ul>
 *   <li>Blank lines and lines whose first non-whitespace character is {@code #} or {@code //} are skipped.</li>
 *   <li>Inline comments (text after {@code #} or {@code //} outside of a string literal) are stripped
 *       before tokenization.</li>
 *   <li>Indentation (leading spaces and tabs) is measured before tokenizing the rest of the line.
 *       Tabs count as 4 spaces. The indentation value is stored on the {@link Line} record, not
 *       as a token.</li>
 *   <li>An identifier starts with a letter or {@code _} and continues with letters, digits,
 *       {@code '}, {@code _}, {@code .}, {@code $}, or {@code /}. This means possessives like
 *       {@code player's} are a single {@link TokenKind#IDENT} token.</li>
 *   <li>A number is a contiguous sequence of digit characters.</li>
 *   <li>A string is delimited by double quotes. The quotes are stripped and backslash escapes
 *       inside the string are accepted (but the escaped characters are kept as-is).</li>
 *   <li>Any other single character becomes a {@link TokenKind#SYMBOL} token.</li>
 * </ul>
 *
 * @see Line
 * @see Token
 */
public final class Tokenizer {

    /**
     * Tokenizes an entire multi-line source string.
     *
     * <p>The source is split on line breaks. Each non-blank, non-comment line is tokenized and
     * wrapped in a {@link Line} record that captures indentation and the 1-based line number.
     *
     * @param src the raw script source text
     * @return an ordered list of tokenized lines; blank and comment lines are excluded
     */
    public List<Line> tokenize(String src) {
        List<Line> lines = new ArrayList<>();
        String[] split = src.split("\\r?\\n", -1);

        for (int i = 0; i < split.length; i++) {
            String raw = split[i];

            int indent = 0;
            int p = 0;
            while (p < raw.length()) {
                char c = raw.charAt(p);
                if (c == ' ') {
                    indent++;
                    p++;
                } else if (c == '\t') {
                    indent += 4;
                    p++;
                } else break;
            }

            String content = raw.substring(p);
            if (content.trim().isEmpty()) continue;
            if (content.trim().startsWith("#") || content.trim().startsWith("//")) continue;
            content = stripInlineComment(content);
            if (content.trim().isEmpty()) continue;

            List<Token> tokens = tokenizeLine(content, i + 1, 0);
            lines.add(new Line(indent, i + 1, content, tokens));
        }

        return lines;
    }

    @SuppressWarnings("SameParameterValue")
    private List<Token> tokenizeLine(String s, int line, int offset) {
        List<Token> out = new ArrayList<>();
        int i = 0;

        while (i < s.length()) {
            char c = s.charAt(i);
            int start = offset + i;

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (c == '"') {
                int j = i + 1;
                StringBuilder sb = new StringBuilder();
                while (j < s.length() && s.charAt(j) != '"') {
                    if (s.charAt(j) == '\\' && j + 1 < s.length()) {
                        j++;
                    }
                    sb.append(s.charAt(j));
                    j++;
                }
                j++;
                out.add(new Token(TokenKind.STRING, sb.toString(), line, start, offset + j));
                i = j;
                continue;
            }

            if (Character.isDigit(c)) {
                int j = i;
                while (j < s.length() && Character.isDigit(s.charAt(j))) j++;
                if (j < s.length() - 1 && s.charAt(j) == '.' && Character.isDigit(s.charAt(j + 1))) {
                    do j++;
                    while (j < s.length() && Character.isDigit(s.charAt(j)));
                }
                out.add(new Token(TokenKind.NUMBER, s.substring(i, j), line, start, offset + j));
                i = j;
                continue;
            }

            if (isIdentStart(c)) {
                int j = i;
                while (j < s.length() && isIdentPart(s.charAt(j))) j++;
                out.add(new Token(TokenKind.IDENT, s.substring(i, j), line, start, offset + j));
                i = j;
                continue;
            }

            out.add(new Token(TokenKind.SYMBOL, String.valueOf(c), line, start, start + 1));
            i++;
        }

        return out;
    }

    private boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentPart(char c) {
        return Character.isLetterOrDigit(c)
                || c == '\''
                || "_.$/".indexOf(c) >= 0;
    }

    private static @NotNull String stripInlineComment(@NotNull String line) {
        boolean inString = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (c == '\\' && inString && i + 1 < line.length()) {
                i++;
                continue;
            }
            if (!inString) {
                if (c == '#') {
                    return line.substring(0, i).stripTrailing();
                }
                if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
                    return line.substring(0, i).stripTrailing();
                }
            }
        }
        return line;
    }

    /**
     * Checks the tokenized lines for indentation consistency and returns any
     * warnings about inconsistent style.
     *
     * <p>This checks two things:
     * <ol>
     *   <li>Whether different indent increments are used (e.g. sometimes +2, sometimes +4)</li>
     *   <li>Whether any line's indent level is not a multiple of the dominant indent width</li>
     * </ol>
     *
     * <p>This method never affects parsing. It only produces human-readable warning messages.
     *
     * @param lines      the tokenized lines to check
     * @param scriptName the script file name for the warning message
     * @return a list of warning messages (empty if indentation is consistent)
     */
    public static @NotNull List<String> checkIndentConsistency(@NotNull List<Line> lines,
                                                               @NotNull String scriptName) {
        if (lines.size() < 2) return List.of();

        Map<Integer, Integer> incrementCounts = new HashMap<>();
        for (int i = 1; i < lines.size(); i++) {
            int diff = lines.get(i).indent() - lines.get(i - 1).indent();
            if (diff > 0) {
                incrementCounts.merge(diff, 1, Integer::sum);
            }
        }
        if (incrementCounts.isEmpty()) return List.of();

        int dominantIncrement = incrementCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .get()
                .getKey();

        List<String> warnings = new ArrayList<>();

        if (incrementCounts.size() > 1) {
            warnings.add("[Script " + scriptName + "] Inconsistent indentation: "
                    + "found indent widths of " + incrementCounts.keySet()
                    + ", expected a consistent indent width of " + dominantIncrement + " spaces");
        }

        for (Line line : lines) {
            if (line.indent() > 0 && line.indent() % dominantIncrement != 0) {
                warnings.add("[Script " + scriptName + "] Line " + line.lineNumber()
                        + ": indent of " + line.indent()
                        + " spaces is not a multiple of the detected indent width ("
                        + dominantIncrement + ")");
            }
        }

        return warnings;
    }
}
