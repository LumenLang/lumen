package dev.lumenlang.lumen.pipeline.language.suggestor.token;

import dev.lumenlang.lumen.pipeline.language.tokenization.Line;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.Tokenizer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Splits a raw line at the cursor column into completed tokens and an {@link ActiveToken}.
 *
 * <p>Active token detection: when the character just before the cursor is whitespace (or the
 * cursor sits at column 0), the active token is empty and its policy is COMPLETE. Otherwise
 * the active token spans from the start of the word the cursor is inside to the cursor itself,
 * with policy PREFIX. Tokens after the cursor (if any) are dropped.
 */
public final class ActiveTokenParser {

    private ActiveTokenParser() {
    }

    /**
     * Parses {@code rawLine} up to {@code cursorCol}. Returns completed tokens followed by the
     * active token descriptor.
     */
    public static @NotNull Parsed parse(@NotNull String rawLine, int cursorCol) {
        int safeCursor = Math.max(0, Math.min(cursorCol, rawLine.length()));
        String before = rawLine.substring(0, safeCursor);
        boolean cursorOnWhitespace = safeCursor == 0 || Character.isWhitespace(rawLine.charAt(safeCursor - 1));
        List<Token> allBefore = tokenize(before);
        if (cursorOnWhitespace || allBefore.isEmpty()) {
            return new Parsed(allBefore, new ActiveToken("", ActiveToken.Policy.COMPLETE, safeCursor, safeCursor));
        }
        Token last = allBefore.get(allBefore.size() - 1);
        List<Token> completed = allBefore.subList(0, allBefore.size() - 1);
        return new Parsed(List.copyOf(completed), new ActiveToken(last.text(), ActiveToken.Policy.PREFIX, last.start(), last.end()));
    }

    private static @NotNull List<Token> tokenize(@NotNull String rawLine) {
        if (rawLine.isBlank()) return List.of();
        List<Line> lines = new Tokenizer().tokenize(rawLine);
        if (lines.isEmpty()) return List.of();
        return lines.get(0).tokens();
    }

    /**
     * Output of the parser.
     *
     * @param completed tokens fully present before the cursor
     * @param active    descriptor for the token at or just before the cursor
     */
    public record Parsed(@NotNull List<Token> completed, @NotNull ActiveToken active) {
    }
}
