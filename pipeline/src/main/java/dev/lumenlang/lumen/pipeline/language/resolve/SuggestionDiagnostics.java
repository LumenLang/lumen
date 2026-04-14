package dev.lumenlang.lumen.pipeline.language.resolve;

import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.pipeline.language.match.MatchProgress;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.TokenKind;
import dev.lumenlang.lumen.pipeline.type.TypeAnnotationParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * Builds a diagnostic from a suggestion, highlighting specific issues found by the simulator.
     *
     * @param errorCode   the diagnostic error code (e.g. "E500", "E502")
     * @param title       the diagnostic title
     * @param line        the source line number
     * @param raw         the raw source text
     * @param tokens      the input tokens that failed matching (used for default highlighting)
     * @param suggestions all ranked suggestions from the simulator (at least one)
     * @return a fully constructed diagnostic
     */
    public static @NotNull LumenDiagnostic build(@NotNull String errorCode, @NotNull String title, int line, @NotNull String raw, @NotNull List<Token> tokens, @NotNull List<PatternSimulator.Suggestion> suggestions) {
        return build(errorCode, title, line, raw, tokens, suggestions.get(0), suggestions.size() > 1 ? suggestions.get(1) : null);
    }

    /**
     * Builds a diagnostic from a single suggestion.
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
        return build(errorCode, title, line, raw, tokens, top, null);
    }

    private static @NotNull LumenDiagnostic build(@NotNull String errorCode, @NotNull String title, int line, @NotNull String raw, @NotNull List<Token> tokens, @NotNull PatternSimulator.Suggestion top, @Nullable PatternSimulator.Suggestion second) {
        LumenDiagnostic unsupported = detectUnsupportedSyntax(line, raw, tokens);
        if (unsupported != null) return unsupported;
        LumenDiagnostic.Builder builder = LumenDiagnostic.error(errorCode, title).at(line, raw);
        if (!tokens.isEmpty()) {
            builder.highlight(tokens.get(0).start(), tokens.get(tokens.size() - 1).end());
        }
        List<PatternSimulator.SuggestionIssue> issues = top.issues();
        PatternSimulator.SuggestionIssue primary = findPrimary(issues);
        if (primary != null) {
            applyPrimaryHighlight(builder, primary, top);
            for (PatternSimulator.SuggestionIssue issue : issues) {
                if (issue == primary) continue;
                applySubHighlight(builder, issue);
            }
        } else if (top.progress() != null && !top.progress().bindingFailures().isEmpty()) {
            List<MatchProgress.BindingFailure> failures = top.progress().bindingFailures();
            MatchProgress.BindingFailure first = failures.get(0);
            if (!first.failedTokens().isEmpty()) {
                Token t = first.failedTokens().get(0);
                String label = fallbackBindingLabel(first, t);
                builder.highlight(t.start(), t.end()).label(label);
            } else {
                Token last = tokens.isEmpty() ? null : tokens.get(tokens.size() - 1);
                if (last != null) builder.highlight(last.end(), last.end() + 1).label("expected " + bindingDescription(first.bindingId()));
                else builder.label("expected " + bindingDescription(first.bindingId()));
            }
            for (int i = 1; i < failures.size(); i++) {
                MatchProgress.BindingFailure bf = failures.get(i);
                if (!bf.failedTokens().isEmpty()) {
                    Token t = bf.failedTokens().get(0);
                    builder.subHighlight(t.start(), t.end(), fallbackBindingLabel(bf, t));
                }
            }
        } else if (top.progress() != null && top.progress().failedBindingId() != null) {
            MatchProgress progress = top.progress();
            Token last = tokens.isEmpty() ? null : tokens.get(tokens.size() - 1);
            if (last != null) {
                builder.highlight(last.end(), last.end() + 1).label("expected " + bindingDescription(progress.failedBindingId()));
            } else {
                builder.label("expected " + bindingDescription(progress.failedBindingId()));
            }
        } else {
            if (!tokens.isEmpty()) {
                builder.label("'" + ExprResolver.joinTokens(tokens) + "' is not recognized");
            } else {
                builder.label("no matching pattern was found");
            }
        }
        builder.note("confidence: " + confidenceTier(top.confidence()));
        builder.help("closest pattern: " + top.pattern().raw());
        if (second != null && !second.pattern().raw().equals(top.pattern().raw())) {
            builder.help("also consider: " + second.pattern().raw());
        }
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
        LumenDiagnostic unsupported = detectUnsupportedSyntax(line, raw, tokens);
        if (unsupported != null) return unsupported;
        LumenDiagnostic.Builder builder = LumenDiagnostic.error(errorCode, title).at(line, raw);
        if (!tokens.isEmpty()) {
            builder.highlight(tokens.get(0).start(), tokens.get(tokens.size() - 1).end()).label("not recognized");
        }
        builder.help("check spelling or ensure the pattern is defined");
        return builder.build();
    }

    private static @Nullable LumenDiagnostic detectUnsupportedSyntax(int line, @NotNull String raw, @NotNull List<Token> tokens) {
        if (tokens.size() == 1 && tokens.get(0).kind() == TokenKind.SYMBOL) {
            String text = tokens.get(0).text();
            if (text.equals("{") || text.equals("}")) {
                return LumenDiagnostic.error("E503", "Unsupported syntax")
                        .at(line, raw)
                        .highlight(tokens.get(0).start(), tokens.get(0).end())
                        .label("curly braces are not part of Lumen syntax")
                        .note("Lumen uses indentation to define blocks, not curly braces")
                        .help("read the documentation at https://docs.lumenlang.dev/ to learn Lumen syntax")
                        .build();
            }
        }
        Token semicolon = null;
        Token brace = null;
        for (Token t : tokens) {
            if (t.kind() != TokenKind.SYMBOL) continue;
            if (semicolon == null && t.text().equals(";")) semicolon = t;
            if (brace == null && (t.text().equals("{") || t.text().equals("}"))) brace = t;
        }
        if (semicolon != null || brace != null) {
            LumenDiagnostic.Builder builder = LumenDiagnostic.error("E503", "Unsupported syntax").at(line, raw);
            if (semicolon != null && brace != null) {
                builder.highlight(semicolon.start(), semicolon.end())
                        .label("semicolons and curly braces are not part of Lumen syntax")
                        .note("Lumen does not use semicolons to end statements or curly braces for blocks");
            } else if (semicolon != null) {
                builder.highlight(semicolon.start(), semicolon.end())
                        .label("semicolons are not part of Lumen syntax")
                        .note("Lumen does not use semicolons to end statements");
            } else {
                builder.highlight(brace.start(), brace.end())
                        .label("curly braces are not part of Lumen syntax")
                        .note("Lumen uses indentation to define blocks, not curly braces");
            }
            builder.help("read the documentation at https://docs.lumenlang.dev/ to learn Lumen syntax");
            return builder.build();
        }
        return null;
    }

    /**
     * Builds a diagnostic from a {@link TypeAnnotationParser.ParseResult.Failure}.
     *
     * @param errorCode the diagnostic error code
     * @param title     the diagnostic title
     * @param line      the source line number
     * @param raw       the raw source text
     * @param tokens    the tokens that were being parsed
     * @param failure   the parse failure result
     * @return a fully constructed diagnostic
     */
    public static @NotNull LumenDiagnostic buildTypeFailure(@NotNull String errorCode, @NotNull String title, int line, @NotNull String raw, @NotNull List<? extends ScriptToken> tokens, @NotNull TypeAnnotationParser.ParseResult.Failure failure) {
        TypeAnnotationParser.ParseError error = failure.error();
        int errorIdx = Math.min(error.tokenOffset(), tokens.size() - 1);
        ScriptToken errorToken = tokens.get(errorIdx);
        LumenDiagnostic.Builder diag = LumenDiagnostic.error(errorCode, title)
                .at(line, raw)
                .highlight(errorToken.start(), errorToken.end());
        if (error.suggestion() != null) diag.label(error.message() + ", did you mean '" + error.suggestion() + "'?");
        else diag.label(error.message());
        diag.help("see https://lumenlang.dev/types for available types");
        return diag.build();
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

    private static @Nullable PatternSimulator.SuggestionIssue findPrimary(@NotNull List<PatternSimulator.SuggestionIssue> issues) {
        PatternSimulator.SuggestionIssue best = null;
        int bestPriority = -1;
        for (PatternSimulator.SuggestionIssue issue : issues) {
            int priority = issuePriority(issue);
            if (priority > bestPriority) {
                bestPriority = priority;
                best = issue;
            }
        }
        return best;
    }

    private static int issuePriority(@NotNull PatternSimulator.SuggestionIssue issue) {
        if (issue instanceof PatternSimulator.SuggestionIssue.Typo) return 4;
        if (issue instanceof PatternSimulator.SuggestionIssue.ExtraTokens) return 3;
        if (issue instanceof PatternSimulator.SuggestionIssue.TypeMismatch) return 2;
        if (issue instanceof PatternSimulator.SuggestionIssue.MissingBinding) return 1;
        if (issue instanceof PatternSimulator.SuggestionIssue.Reorder) return 1;
        return 0;
    }

    private static void applyPrimaryHighlight(@NotNull LumenDiagnostic.Builder builder, @NotNull PatternSimulator.SuggestionIssue issue, @NotNull PatternSimulator.Suggestion top) {
        if (issue instanceof PatternSimulator.SuggestionIssue.Typo typo) {
            boolean validated = top.progress() != null && top.progress().succeeded();
            builder.highlight(typo.token().start(), typo.token().end());
            builder.label(validated ? "replace with '" + typo.expected() + "'" : "did you mean '" + typo.expected() + "'?");
        } else if (issue instanceof PatternSimulator.SuggestionIssue.ExtraTokens extra) {
            Token first = extra.tokens().get(0);
            if (extra.tokens().size() == 1) {
                builder.highlight(first.start(), first.end()).label("remove '" + first.text() + "'");
            } else {
                Token last = extra.tokens().get(extra.tokens().size() - 1);
                builder.highlight(first.start(), last.end()).label("remove " + extra.tokens().size() + " extra tokens");
            }
        } else if (issue instanceof PatternSimulator.SuggestionIssue.TypeMismatch mismatch) {
            builder.highlight(mismatch.token().start(), mismatch.token().end());
            builder.label(typeMismatchLabel(mismatch));
        } else if (issue instanceof PatternSimulator.SuggestionIssue.MissingBinding missing) {
            builder.label("missing " + bindingDescription(missing.bindingId()));
        } else if (issue instanceof PatternSimulator.SuggestionIssue.Reorder reorder) {
            int start = reorder.tokens().stream().mapToInt(Token::start).min().orElse(0);
            int end = reorder.tokens().stream().mapToInt(Token::end).max().orElse(0);
            builder.highlight(start, end).label("tokens '" + reorderNote(reorder.tokens()) + "' may be in the wrong order");
        }
    }

    private static void applySubHighlight(@NotNull LumenDiagnostic.Builder builder, @NotNull PatternSimulator.SuggestionIssue issue) {
        if (issue instanceof PatternSimulator.SuggestionIssue.Typo typo) {
            builder.subHighlight(typo.token().start(), typo.token().end(), "did you mean '" + typo.expected() + "'?");
        } else if (issue instanceof PatternSimulator.SuggestionIssue.ExtraTokens extra) {
            for (Token t : extra.tokens()) {
                builder.subHighlight(t.start(), t.end(), "extra token");
            }
        } else if (issue instanceof PatternSimulator.SuggestionIssue.TypeMismatch mismatch) {
            builder.subHighlight(mismatch.token().start(), mismatch.token().end(), typeMismatchLabel(mismatch));
        } else if (issue instanceof PatternSimulator.SuggestionIssue.MissingBinding missing) {
            builder.note("missing " + bindingDescription(missing.bindingId()));
        } else if (issue instanceof PatternSimulator.SuggestionIssue.Reorder reorder) {
            for (Token t : reorder.tokens()) {
                builder.subHighlight(t.start(), t.end(), "out of order");
            }
        }
    }

    private static @NotNull String confidenceTier(double confidence) {
        int pct = (int) Math.round(confidence * 100);
        String tier;
        if (pct >= 90) tier = "very high";
        else if (pct >= 70) tier = "high";
        else if (pct >= 50) tier = "moderate";
        else if (pct >= 30) tier = "low";
        else tier = "very low";
        return pct + "% (" + tier + ")";
    }

    private static @NotNull String typeMismatchLabel(@NotNull PatternSimulator.SuggestionIssue.TypeMismatch mismatch) {
        if (mismatch.reason() != null) return mismatch.reason();
        String token = mismatch.token().text();
        return switch (mismatch.bindingId()) {
            case "INVENTORY" -> "'" + token + "' is not a known inventory variable";
            case "PLAYER" -> "'" + token + "' is not a known player variable";
            case "ENTITY" -> "'" + token + "' is not a known entity variable";
            case "ENTITY_POSSESSIVE" -> "'" + token + "' must use possessive form (e.g. entity's)";
            case "ITEMSTACK" -> "'" + token + "' is not a known item variable";
            case "WORLD" -> "'" + token + "' is not a known world variable";
            case "LIST" -> "'" + token + "' is not a known list variable";
            case "MAP" -> "'" + token + "' is not a known map variable";
            case "DATA" -> "'" + token + "' is not a known data variable";
            default -> "'" + token + "' is not valid here";
        };
    }

    private static @NotNull String fallbackBindingLabel(@NotNull MatchProgress.BindingFailure bf, @NotNull Token t) {
        if (bf.reason() != null) return bf.reason();
        return "'" + t.text() + "' is not " + bindingDescription(bf.bindingId());
    }

    private static @NotNull String bindingDescription(@NotNull String bindingId) {
        return switch (bindingId) {
            case "PLAYER" -> "a player variable";
            case "ENTITY" -> "an entity variable";
            case "ENTITY_POSSESSIVE" -> "a possessive entity reference (e.g. entity's)";
            case "ITEMSTACK" -> "an item variable";
            case "INVENTORY" -> "an inventory variable";
            case "WORLD" -> "a world variable";
            case "LOCATION" -> "a location variable";
            case "BLOCK" -> "a block variable";
            case "LIST" -> "a list variable";
            case "MAP" -> "a map variable";
            case "DATA" -> "a data variable";
            case "INT" -> "a valid integer";
            case "DOUBLE", "NUMBER" -> "a valid number";
            case "LONG" -> "a valid long";
            case "BOOLEAN" -> "a boolean (true or false)";
            case "MATERIAL" -> "a valid material";
            case "ATTRIBUTE" -> "a valid attribute";
            case "ENTITY_TYPE" -> "a valid entity type";
            case "GAME_MODE" -> "a valid game mode";
            case "STRING" -> "a string value";
            case "QSTRING" -> "a quoted string, variable, or placeholder";
            case "COND" -> "a condition";
            case "EXPR" -> "an expression";
            case "VAR" -> "a variable";
            default -> "a valid " + bindingId.toLowerCase().replace('_', ' ');
        };
    }
}
