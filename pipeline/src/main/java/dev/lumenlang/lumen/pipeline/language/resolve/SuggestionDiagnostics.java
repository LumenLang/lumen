package dev.lumenlang.lumen.pipeline.language.resolve;

import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.type.NullableType;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.match.MatchProgress;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.TokenKind;
import dev.lumenlang.lumen.pipeline.type.TypeAnnotationParser;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Shared utility for building diagnostics from {@link PatternSimulator.Suggestion} results.
 */
public final class SuggestionDiagnostics {

    private SuggestionDiagnostics() {
    }

    /**
     * Builds a diagnostic from a suggestion, highlighting specific issues found by the simulator.
     *
     * @param title       the diagnostic title
     * @param line        the source line number
     * @param raw         the raw source text
     * @param tokens      the input tokens that failed matching (used for default highlighting)
     * @param suggestions all ranked suggestions from the simulator (at least one)
     * @return a fully constructed diagnostic
     */
    public static @NotNull LumenDiagnostic build(@NotNull String title, int line, @NotNull String raw, @NotNull List<Token> tokens, @NotNull List<PatternSimulator.Suggestion> suggestions) {
        return build(title, line, raw, tokens, suggestions.get(0), suggestions.size() > 1 ? suggestions.get(1) : null, null);
    }

    /**
     * Builds a diagnostic from suggestions with nullable variable awareness.
     *
     * @param title       the diagnostic title
     * @param line        the source line number
     * @param raw         the raw source text
     * @param tokens      the input tokens that failed matching
     * @param suggestions all ranked suggestions from the simulator
     * @param env         the type environment for nullable variable detection
     * @return a fully constructed diagnostic with nullable hints if applicable
     */
    public static @NotNull LumenDiagnostic build(@NotNull String title, int line, @NotNull String raw, @NotNull List<Token> tokens, @NotNull List<PatternSimulator.Suggestion> suggestions, @Nullable TypeEnv env) {
        return build(title, line, raw, tokens, suggestions.get(0), suggestions.size() > 1 ? suggestions.get(1) : null, env);
    }

    /**
     * Builds a diagnostic from a single suggestion.
     *
     * @param title  the diagnostic title
     * @param line   the source line number
     * @param raw    the raw source text
     * @param tokens the input tokens that failed matching (used for default highlighting)
     * @param top    the best suggestion from the simulator
     * @return a fully constructed diagnostic
     */
    public static @NotNull LumenDiagnostic build(@NotNull String title, int line, @NotNull String raw, @NotNull List<Token> tokens, @NotNull PatternSimulator.Suggestion top) {
        return build(title, line, raw, tokens, top, null, null);
    }

    private static @NotNull LumenDiagnostic build(@NotNull String title, int line, @NotNull String raw, @NotNull List<Token> tokens, @NotNull PatternSimulator.Suggestion top, @Nullable PatternSimulator.Suggestion second, @Nullable TypeEnv env) {
        LumenDiagnostic unsupported = detectUnsupportedSyntax(line, raw, tokens);
        if (unsupported != null) return unsupported;
        TypeRegistry types = PatternRegistry.instance().getTypeRegistry();
        LumenDiagnostic.Builder builder = LumenDiagnostic.error(title).at(line, raw);
        if (!tokens.isEmpty()) {
            builder.highlight(tokens.get(0).start(), tokens.get(tokens.size() - 1).end());
        }
        List<PatternSimulator.SuggestionIssue> issues = top.issues();
        PatternSimulator.SuggestionIssue primary = findPrimary(issues);
        if (primary != null) {
            applyPrimaryHighlight(builder, primary, top, types);
            for (PatternSimulator.SuggestionIssue issue : issues) {
                if (issue == primary) continue;
                applySubHighlight(builder, issue, types);
            }
        } else if (top.progress() != null && !top.progress().bindingFailures().isEmpty()) {
            List<MatchProgress.BindingFailure> failures = top.progress().bindingFailures();
            MatchProgress.BindingFailure first = failures.get(0);
            if (!first.failedTokens().isEmpty()) {
                Token t = first.failedTokens().get(0);
                builder.highlight(t.start(), t.end()).label(first.reason());
            } else {
                Token last = tokens.isEmpty() ? null : tokens.get(tokens.size() - 1);
                String label = "expected " + types.displayNameOf(first.bindingId());
                if (last != null) builder.highlight(last.end(), last.end() + 1).label(label);
                else builder.label(label);
            }
            for (int i = 1; i < failures.size(); i++) {
                MatchProgress.BindingFailure bf = failures.get(i);
                if (!bf.failedTokens().isEmpty()) {
                    Token t = bf.failedTokens().get(0);
                    builder.subHighlight(t.start(), t.end(), bf.reason());
                }
            }
        } else if (top.progress() != null && top.progress().failedBindingId() != null) {
            MatchProgress progress = top.progress();
            Token last = tokens.isEmpty() ? null : tokens.get(tokens.size() - 1);
            String reason = progress.failedReason();
            String label = reason != null ? reason : "expected " + types.displayNameOf(progress.failedBindingId());
            if (last != null) builder.highlight(last.end(), last.end() + 1).label(label);
            else builder.label(label);
        } else {
            if (!tokens.isEmpty()) {
                builder.label("'" + ExprResolver.joinTokens(tokens) + "' is not recognized");
            } else {
                builder.label("no matching pattern was found");
            }
        }
        appendNullableHints(builder, tokens, env);
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
     * @param title  the diagnostic title
     * @param line   the source line number
     * @param raw    the raw source text
     * @param tokens the input tokens
     * @return a fully constructed diagnostic
     */
    public static @NotNull LumenDiagnostic buildNoSuggestion(@NotNull String title, int line, @NotNull String raw, @NotNull List<Token> tokens) {
        return buildNoSuggestion(title, line, raw, tokens, null);
    }

    /**
     * Builds a diagnostic when no suggestion was found, with nullable variable awareness.
     *
     * @param title  the diagnostic title
     * @param line   the source line number
     * @param raw    the raw source text
     * @param tokens the input tokens
     * @param env    the type environment for nullable variable detection
     * @return a fully constructed diagnostic with nullable hints if applicable
     */
    public static @NotNull LumenDiagnostic buildNoSuggestion(@NotNull String title, int line, @NotNull String raw, @NotNull List<Token> tokens, @Nullable TypeEnv env) {
        LumenDiagnostic unsupported = detectUnsupportedSyntax(line, raw, tokens);
        if (unsupported != null) return unsupported;
        LumenDiagnostic.Builder builder = LumenDiagnostic.error(title).at(line, raw);
        if (!tokens.isEmpty()) {
            builder.highlight(tokens.get(0).start(), tokens.get(tokens.size() - 1).end()).label("not recognized");
        }
        appendNullableHints(builder, tokens, env);
        builder.help("check spelling or ensure the pattern is defined");
        return builder.build();
    }

    private static void appendNullableHints(@NotNull LumenDiagnostic.Builder builder, @NotNull List<Token> tokens, @Nullable TypeEnv env) {
        if (env == null) return;
        for (Token t : tokens) {
            if (t.kind() != TokenKind.IDENT) continue;
            EnvironmentAccess.VarHandle ref = env.lookupVar(t.text());
            if (ref == null) continue;
            if (!(ref.type() instanceof NullableType)) continue;
            TypeEnv.NullState state = env.nullState(ref.java());
            if (state == TypeEnv.NullState.NON_NULL) {
                builder.note("'" + t.text() + "' has type '" + ref.type().displayName() + "' but is narrowed to non-null in this scope");
                continue;
            }
            builder.note("'" + t.text() + "' has nullable type '" + ref.type().displayName() + "' and may be null");
            builder.help("narrow with 'if " + t.text() + " is set:' before using it");
            return;
        }
    }

    private static @Nullable LumenDiagnostic detectUnsupportedSyntax(int line, @NotNull String raw, @NotNull List<Token> tokens) {
        if (tokens.size() == 1 && tokens.get(0).kind() == TokenKind.SYMBOL) {
            String text = tokens.get(0).text();
            if (text.equals("{") || text.equals("}")) {
                return LumenDiagnostic.error("Unsupported syntax")
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
            LumenDiagnostic.Builder builder = LumenDiagnostic.error("Unsupported syntax").at(line, raw);
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
     * @param title   the diagnostic title
     * @param line    the source line number
     * @param raw     the raw source text
     * @param tokens  the tokens that were being parsed
     * @param failure the parse failure result
     * @return a fully constructed diagnostic
     */
    public static @NotNull LumenDiagnostic buildTypeFailure(@NotNull String title, int line, @NotNull String raw, @NotNull List<? extends ScriptToken> tokens, @NotNull TypeAnnotationParser.ParseResult.Failure failure) {
        TypeAnnotationParser.ParseError error = failure.error();
        int errorIdx = Math.min(error.tokenOffset(), tokens.size() - 1);
        ScriptToken errorToken = tokens.get(errorIdx);
        LumenDiagnostic.Builder diag = LumenDiagnostic.error(title)
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

    private static void applyPrimaryHighlight(@NotNull LumenDiagnostic.Builder builder, @NotNull PatternSimulator.SuggestionIssue issue, @NotNull PatternSimulator.Suggestion top, @NotNull TypeRegistry types) {
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
            builder.label(mismatch.reason());
        } else if (issue instanceof PatternSimulator.SuggestionIssue.MissingBinding missing) {
            builder.label("missing " + types.displayNameOf(missing.bindingId()));
        } else if (issue instanceof PatternSimulator.SuggestionIssue.Reorder reorder) {
            int start = reorder.tokens().stream().mapToInt(Token::start).min().orElse(0);
            int end = reorder.tokens().stream().mapToInt(Token::end).max().orElse(0);
            builder.highlight(start, end).label("tokens '" + reorderNote(reorder.tokens()) + "' may be in the wrong order");
        }
    }

    private static void applySubHighlight(@NotNull LumenDiagnostic.Builder builder, @NotNull PatternSimulator.SuggestionIssue issue, @NotNull TypeRegistry types) {
        if (issue instanceof PatternSimulator.SuggestionIssue.Typo typo) {
            builder.subHighlight(typo.token().start(), typo.token().end(), "did you mean '" + typo.expected() + "'?");
        } else if (issue instanceof PatternSimulator.SuggestionIssue.ExtraTokens extra) {
            for (Token t : extra.tokens()) {
                builder.subHighlight(t.start(), t.end(), "extra token");
            }
        } else if (issue instanceof PatternSimulator.SuggestionIssue.TypeMismatch mismatch) {
            builder.subHighlight(mismatch.token().start(), mismatch.token().end(), mismatch.reason());
        } else if (issue instanceof PatternSimulator.SuggestionIssue.MissingBinding missing) {
            builder.note("missing " + types.displayNameOf(missing.bindingId()));
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
}
