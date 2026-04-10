package dev.lumenlang.lumen.pipeline.language.resolve;

import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.pipeline.language.match.MatchProgress;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Shared utility for building diagnostics from {@link PatternSimulator.Suggestion} results.
 *
 * <p>Consolidates the diagnostic building logic previously duplicated in CodeEmitter,
 * VarDeclarationForm, and GlobalDeclarationForm into a single location.
 */
public final class SuggestionDiagnostics {

    private SuggestionDiagnostics() {
    }

    /**
     * Builds a diagnostic from a suggestion, highlighting the specific failure point.
     *
     * @param errorCode the diagnostic error code (e.g. "E500", "E502")
     * @param title     the diagnostic title
     * @param line      the source line number
     * @param raw       the raw source text
     * @param tokens    the input tokens that failed matching (used for default highlighting)
     * @param top       the best suggestion from the simulator
     * @return a fully constructed diagnostic
     */
    public static @NotNull LumenDiagnostic build(@NotNull String errorCode, @NotNull String title, int line, @NotNull String raw, @NotNull List<Token> tokens, @NotNull PatternSimulator.Suggestion top) {
        LumenDiagnostic.Builder builder = LumenDiagnostic.error(errorCode, title).at(line, raw);
        if (!tokens.isEmpty()) {
            builder.highlight(tokens.get(0).start(), tokens.get(tokens.size() - 1).end());
        }
        switch (top.kind()) {
            case TYPO, TYPO_AND_REORDER -> {
                if (top.errorToken() != null) {
                    boolean validated = top.progress() != null && top.progress().succeeded();
                    builder.highlight(top.errorToken().start(), top.errorToken().end()).label(validated ? "replace with '" + top.expectedText() + "'" : "did you mean '" + top.expectedText() + "'?");
                    if (top.kind() == PatternSimulator.SuggestionKind.TYPO_AND_REORDER && !top.reorderedTokens().isEmpty()) {
                        String reorderVerb = validated ? "are" : "appear to be";
                        builder.note("tokens '" + reorderNote(top.reorderedTokens()) + "' " + reorderVerb + " in the wrong order");
                    }
                }
            }
            case REORDER -> {
                if (!top.reorderedTokens().isEmpty()) {
                    List<Token> reordered = top.reorderedTokens();
                    int start = reordered.stream().mapToInt(Token::start).min().orElse(0);
                    int end = reordered.stream().mapToInt(Token::end).max().orElse(0);
                    boolean validated = top.progress() != null && top.progress().succeeded();
                    String verb = validated ? "are" : "appear to be";
                    builder.highlight(start, end).label("tokens '" + reorderNote(reordered) + "' " + verb + " in the wrong order");
                }
            }
            case TYPE_MISMATCH -> {
                MatchProgress progress = top.progress();
                if (progress != null && progress.failedBindingId() != null && !progress.failedTokens().isEmpty()) {
                    Token failedAt = progress.failedTokens().get(0);
                    String label = "type '" + progress.failedBindingId() + "' cannot parse this";
                    if (progress.failedReason() != null) label += ": " + progress.failedReason();
                    builder.highlight(failedAt.start(), failedAt.end()).label(label);
                } else if (progress != null && progress.failedBindingId() != null) {
                    Token last = tokens.isEmpty() ? null : tokens.get(tokens.size() - 1);
                    if (last != null) {
                        builder.highlight(last.end(), last.end() + 1).label("expected " + progress.failedBindingId() + " here");
                    } else {
                        builder.label("expected a value of type '" + progress.failedBindingId() + "'");
                    }
                } else if (progress != null && !progress.failedTokens().isEmpty()) {
                    Token failedAt = progress.failedTokens().get(0);
                    builder.highlight(failedAt.start(), failedAt.end()).label("unexpected token '" + failedAt.text() + "'");
                } else {
                    builder.label("pattern matches structurally but a type binding failed");
                }
            }
            default -> {
                if (!tokens.isEmpty()) {
                    builder.label("'" + ExprResolver.joinTokens(tokens) + "' is not recognized");
                } else {
                    builder.label("no matching pattern was found");
                }
            }
        }
        builder.help("closest pattern: " + top.pattern().raw());
        return builder.build();
    }

    /**
     * Builds a diagnostic when no suggestion was found at all.
     *
     * @param errorCode the diagnostic error code
     * @param title     the diagnostic title
     * @param line      the source line number
     * @param raw       the raw source text
     * @param tokens    the input tokens
     * @return a fully constructed diagnostic
     */
    public static @NotNull LumenDiagnostic buildNoSuggestion(@NotNull String errorCode, @NotNull String title, int line, @NotNull String raw, @NotNull List<Token> tokens) {
        LumenDiagnostic.Builder builder = LumenDiagnostic.error(errorCode, title).at(line, raw);
        if (!tokens.isEmpty()) {
            builder.highlight(tokens.get(0).start(), tokens.get(tokens.size() - 1).end()).label("not recognized");
        }
        builder.help("check spelling or ensure the pattern is defined");
        return builder.build();
    }

    /**
     * Formats reordered tokens into a human readable description for diagnostic labels.
     *
     * @param reordered the tokens that appear in the wrong order
     * @return formatted description
     */
    public static @NotNull String reorderNote(@NotNull List<Token> reordered) {
        if (reordered.size() == 2) return reordered.get(0).text() + "' and '" + reordered.get(1).text();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reordered.size(); i++) {
            if (i > 0) sb.append(i == reordered.size() - 1 ? "' and '" : "', '");
            sb.append(reordered.get(i).text());
        }
        return sb.toString();
    }
}
