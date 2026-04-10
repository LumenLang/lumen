package dev.lumenlang.lumen.pipeline.language.resolve;

import dev.lumenlang.lumen.api.codegen.JavaOutput;
import dev.lumenlang.lumen.api.pattern.PatternMeta;
import dev.lumenlang.lumen.pipeline.codegen.BindingContext;
import dev.lumenlang.lumen.pipeline.codegen.BlockContext;
import dev.lumenlang.lumen.pipeline.codegen.CodegenContext;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.conditions.registry.RegisteredCondition;
import dev.lumenlang.lumen.pipeline.language.match.Match;
import dev.lumenlang.lumen.pipeline.language.match.MatchProgress;
import dev.lumenlang.lumen.pipeline.language.match.PatternMatcher;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternPart;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredBlock;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredExpression;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredPattern;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.TokenKind;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import dev.lumenlang.lumen.pipeline.util.FuzzyMatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Finds near matches when input tokens fail all normal matching paths.
 *
 * <p>This class is invoked after a statement, expression, condition, or block fails to match
 * any registered pattern through the normal pipeline. It scores every registered pattern against
 * the failed input and returns the best ranked suggestions for diagnostic reporting.
 *
 * <h2>Pipeline</h2>
 * <ol>
 *   <li><b>Fuzzy literal scoring</b> ({@code scoreAgainst}): extracts required and optional
 *       literals from each pattern and fuzzy matches them against input tokens using edit distance.
 *       Produces a {@link CandidateScore} with typo detection, reorder detection, and an overall
 *       confidence score. Patterns that score below {@link #MIN_SCORE} are discarded. The top
 *       {@link #MAX_REAL_CANDIDATES} candidates advance to real matching.</li>
 *   <li><b>Real matching</b> ({@code matchWithProgress}): for each top candidate, runs the real
 *       {@link PatternMatcher} against the original tokens to capture a {@link MatchProgress}
 *       describing exactly where and why matching failed (binding ID, rejection reason, failed
 *       tokens). If this succeeds, the candidate is silently skipped (it matched normally).</li>
 *   <li><b>Corrected token validation</b> ({@code tryCorrectedMatch}): when a typo is detected,
 *       replaces the typo token with the expected literal and re runs real matching. If the
 *       corrected tokens match successfully, the suggestion is validated with high confidence.
 *       If the corrected tokens still fail, the candidate is skipped entirely (the pattern does
 *       not work for this input even with the typo fixed).</li>
 *   <li><b>Shape matching</b> ({@code tryShapeMatch}): anchors fuzzy matched literals at their
 *       detected positions, assigns remaining tokens to placeholders in pattern order, and
 *       re runs real matching on the rearranged sequence. If the rearranged tokens match
 *       successfully, the suggestion is a validated reorder. If reorder was detected by fuzzy
 *       scoring but the shape match fails with a type binding error, the failure details are
 *       preserved. If the shape match fails completely for a reorder candidate, the candidate
 *       is skipped (the pattern does not work even with corrected order).</li>
 *   <li><b>Handler sandbox</b> ({@code tryHandlerSandbox}): for builtin patterns (registered by
 *       Lumen core via {@code .by("Lumen")}), after a corrected match succeeds, executes the
 *       handler against a noop output to verify the match would produce valid code generation.
 *       If the handler throws, the candidate is skipped. Addon handlers are not sandboxed.</li>
 * </ol>
 *
 * <h2>Candidate Elimination</h2>
 * <p>Each validation step can eliminate a candidate by calling {@code continue}, which causes
 * the loop to try the next highest scoring candidate from fuzzy scoring. This prevents the
 * simulator from suggesting patterns that look similar on the surface but would never actually
 * work for the given input tokens.
 */
public final class PatternSimulator {

    private static final int MAX_SUGGESTIONS = 2;
    private static final int MAX_REAL_CANDIDATES = 5;
    private static final double MIN_SCORE = 10.0;
    private static final int SHAPE_MATCH_TOKEN_LIMIT = 14;

    private PatternSimulator() {
    }

    /**
     * Finds the closest matching expression patterns for unrecognized input tokens.
     *
     * @param tokens the tokens that failed expression matching
     * @param reg    the pattern registry
     * @param env    the type environment
     * @return ranked suggestions (at most 2), or empty if nothing is close
     */
    public static @NotNull List<Suggestion> suggestExpressions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnv env) {
        List<CandidateScore> scored = new ArrayList<>();
        for (RegisteredExpression re : reg.getExpressions()) {
            CandidateScore cs = scoreAgainst(tokens, re.pattern(), re.meta(), re);
            if (cs != null && cs.score >= MIN_SCORE) scored.add(cs);
        }
        return simulateAndBuild(scored, tokens, reg.getTypeRegistry(), env);
    }

    /**
     * Finds the closest matching condition patterns for unrecognized input tokens.
     *
     * @param tokens the tokens that failed condition matching
     * @param reg    the pattern registry
     * @param env    the type environment
     * @return ranked suggestions (at most 2), or empty if nothing is close
     */
    public static @NotNull List<Suggestion> suggestConditions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnv env) {
        List<CandidateScore> scored = new ArrayList<>();
        for (RegisteredCondition rc : reg.getConditionRegistry().getConditions()) {
            CandidateScore cs = scoreAgainst(tokens, rc.pattern(), rc.meta(), null);
            if (cs != null && cs.score >= MIN_SCORE) scored.add(cs);
        }
        return simulateAndBuild(scored, tokens, reg.getTypeRegistry(), env);
    }

    /**
     * Finds the closest matching block patterns for unrecognized input tokens.
     *
     * @param tokens the tokens that failed block matching
     * @param reg    the pattern registry
     * @param env    the type environment
     * @return ranked suggestions (at most 2), or empty if nothing is close
     */
    public static @NotNull List<Suggestion> suggestBlocks(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnv env) {
        List<CandidateScore> scored = new ArrayList<>();
        for (RegisteredBlock rb : reg.getBlocks()) {
            CandidateScore cs = scoreAgainst(tokens, rb.pattern(), rb.meta(), null);
            if (cs != null && cs.score >= MIN_SCORE) scored.add(cs);
        }
        return simulateAndBuild(scored, tokens, reg.getTypeRegistry(), env);
    }

    /**
     * Finds the closest matching patterns across both statements and expressions.
     *
     * @param tokens the tokens that failed all matching
     * @param reg    the pattern registry
     * @param env    the type environment
     * @return ranked suggestions (at most 2), or empty if nothing is close
     */
    public static @NotNull List<Suggestion> suggestStatementsAndExpressions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnv env) {
        List<CandidateScore> scored = new ArrayList<>();
        for (RegisteredPattern rp : reg.getStatements()) {
            CandidateScore cs = scoreAgainst(tokens, rp.pattern(), rp.meta(), rp);
            if (cs != null && cs.score >= MIN_SCORE) scored.add(cs);
        }
        for (RegisteredExpression re : reg.getExpressions()) {
            CandidateScore cs = scoreAgainst(tokens, re.pattern(), re.meta(), re);
            if (cs != null && cs.score >= MIN_SCORE) scored.add(cs);
        }
        return simulateAndBuild(scored, tokens, reg.getTypeRegistry(), env);
    }

    private static @NotNull List<Suggestion> simulateAndBuild(@NotNull List<CandidateScore> scored, @NotNull List<Token> tokens, @NotNull TypeRegistry types, @NotNull TypeEnv env) {
        scored.sort(Comparator.comparingDouble((CandidateScore c) -> c.score).reversed());
        List<Suggestion> results = new ArrayList<>();
        int limit = Math.min(MAX_REAL_CANDIDATES, scored.size());
        for (int i = 0; i < limit && results.size() < MAX_SUGGESTIONS; i++) {
            CandidateScore cs = scored.get(i);

            MatchProgress progress = null;
            if (tokens.size() <= 20) {
                progress = PatternMatcher.matchWithProgress(tokens, cs.pattern, types, env);
                if (progress.succeeded()) continue;
            }

            boolean hasTypo = cs.typoToken != null;
            boolean hasReorder = !cs.reorderedTokens.isEmpty();

            if (hasTypo && tokens.size() <= 20) {
                MatchProgress corrected = tryCorrectedMatch(tokens, cs, types, env);
                if (corrected.succeeded()) {
                    if (isBuiltin(cs.meta) && corrected.match() != null && !tryHandlerSandbox(cs.handler, corrected.match(), env)) {
                        continue;
                    }
                    SuggestionKind kind = hasReorder ? SuggestionKind.TYPO_AND_REORDER : SuggestionKind.TYPO;
                    String detail = "did you mean '" + cs.expectedText + "'?";
                    if (hasReorder)
                        detail += " tokens '" + reorderDescription(cs.reorderedTokens) + "' appear to be in the wrong order";
                    results.add(new Suggestion(cs.pattern, kind, cs.score + 40, detail, cs.typoToken, cs.expectedText, cs.reorderedTokens, corrected));
                }
                continue;
            }

            boolean shapeMatchSucceeded = false;
            long anchoredCount = cs.matchDetails.stream().filter(m -> m.tokenIndex >= 0).count();
            if (tokens.size() <= SHAPE_MATCH_TOKEN_LIMIT && anchoredCount >= 1) {
                MatchProgress shaped = tryShapeMatch(tokens, cs.pattern, cs.matchDetails, types, env);
                if (shaped != null) {
                    if (shaped.succeeded()) {
                        shapeMatchSucceeded = true;
                        progress = shaped;
                    } else if (hasReorder) {
                        if (shaped.failedBindingId() != null && shaped.furthestTokenIndex() > 0) {
                            progress = shaped;
                        } else {
                            continue;
                        }
                    } else if (progress == null || shaped.furthestTokenIndex() > progress.furthestTokenIndex()) {
                        progress = shaped;
                    }
                }
            }

            SuggestionKind kind;
            String detail;

            if (shapeMatchSucceeded) {
                kind = SuggestionKind.REORDER;
                List<Token> reordered = findReorderedFromAnchors(tokens, cs.matchDetails);
                if (!reordered.isEmpty()) {
                    detail = "tokens '" + reorderDescription(reordered) + "' appear to be in the wrong order";
                } else {
                    detail = "tokens appear to be in the wrong order";
                }
                results.add(new Suggestion(cs.pattern, kind, cs.score + 30, detail, cs.typoToken, cs.expectedText, reordered.isEmpty() ? cs.reorderedTokens : reordered, progress));
                continue;
            } else if (progress != null && progress.furthestTokenIndex() > tokens.size() / 2) {
                kind = SuggestionKind.TYPE_MISMATCH;
                if (progress.failedBindingId() != null) {
                    detail = "type binding '" + progress.failedBindingId() + "' rejected the input";
                } else if (!progress.failedTokens().isEmpty()) {
                    detail = "unexpected token '" + progress.failedTokens().get(0).text() + "'";
                } else {
                    detail = "pattern almost matched";
                }
            } else if (hasTypo && hasReorder) {
                kind = SuggestionKind.TYPO_AND_REORDER;
                detail = "did you mean '" + cs.expectedText + "'? tokens '" + reorderDescription(cs.reorderedTokens) + "' appear to be in the wrong order";
            } else if (hasTypo) {
                kind = SuggestionKind.TYPO;
                detail = "did you mean '" + cs.expectedText + "'?";
            } else if (hasReorder) {
                kind = SuggestionKind.REORDER;
                detail = "tokens '" + reorderDescription(cs.reorderedTokens) + "' appear to be in the wrong order";
            } else if (cs.kind == SuggestionKind.TYPE_MISMATCH) {
                kind = SuggestionKind.TYPE_MISMATCH;
                detail = "pattern matches structurally but a type binding failed";
            } else {
                kind = SuggestionKind.CLOSE_MATCH;
                detail = "closest matching pattern";
            }
            results.add(new Suggestion(cs.pattern, kind, cs.score, detail, cs.typoToken, cs.expectedText, cs.reorderedTokens, progress));
        }
        return results;
    }

    /**
     * Tries to match tokens against a pattern by anchoring known literal positions
     * and filling placeholders with the remaining tokens. This catches cases where
     * the token order roughly matches the pattern structure but the raw match fails
     * due to a typo or one swapped token breaking backtracking.
     */
    private static @Nullable MatchProgress tryShapeMatch(@NotNull List<Token> tokens, @NotNull Pattern pattern, @NotNull List<LiteralMatchResult> matchDetails, @NotNull TypeRegistry types, @NotNull TypeEnv env) {
        List<LiteralMatchResult> anchored = matchDetails.stream().filter(m -> m.tokenIndex >= 0).sorted(Comparator.comparingInt(m -> m.literal.partIndex)).toList();
        if (anchored.isEmpty()) return null;

        boolean[] used = new boolean[tokens.size()];
        for (LiteralMatchResult m : anchored) {
            if (m.tokenIndex >= 0 && m.tokenIndex < tokens.size()) used[m.tokenIndex] = true;
        }

        List<Token> remaining = new ArrayList<>();
        for (int j = 0; j < tokens.size(); j++) {
            if (!used[j]) remaining.add(tokens.get(j));
        }

        List<PatternPart> parts = pattern.parts();
        List<Token> shaped = new ArrayList<>();
        int remIdx = 0;

        for (PatternPart part : parts) {
            if (part instanceof PatternPart.Literal lit) {
                Token anchor = findAnchorToken(anchored, tokens, lit.text());
                shaped.add(anchor != null ? anchor : syntheticToken(lit.text(), tokens));
            } else if (part instanceof PatternPart.FlexLiteral flex) {
                Token anchor = findFlexAnchorToken(anchored, tokens, flex.forms());
                shaped.add(anchor != null ? anchor : syntheticToken(flex.forms().get(0), tokens));
            } else if (part instanceof PatternPart.PlaceholderPart) {
                if (remIdx < remaining.size()) {
                    shaped.add(remaining.get(remIdx++));
                }
            } else if (part instanceof PatternPart.Group group) {
                if (!group.required()) continue;
                for (List<PatternPart> alt : group.alternatives()) {
                    for (PatternPart ap : alt) {
                        if (ap instanceof PatternPart.Literal gl) {
                            Token a = findAnchorToken(anchored, tokens, gl.text());
                            if (a != null) {
                                shaped.add(a);
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
        while (remIdx < remaining.size()) {
            shaped.add(remaining.get(remIdx++));
        }

        if (shaped.isEmpty()) return null;
        return PatternMatcher.matchWithProgress(shaped, pattern, types, env);
    }

    private static @Nullable Token findAnchorToken(@NotNull List<LiteralMatchResult> anchored, @NotNull List<Token> tokens, @NotNull String text) {
        for (LiteralMatchResult m : anchored) {
            if (m.literal.text.equalsIgnoreCase(text) && m.tokenIndex >= 0 && m.tokenIndex < tokens.size()) {
                return tokens.get(m.tokenIndex);
            }
        }
        return null;
    }

    private static @Nullable Token findFlexAnchorToken(@NotNull List<LiteralMatchResult> anchored, @NotNull List<Token> tokens, @NotNull List<String> forms) {
        for (String form : forms) {
            Token found = findAnchorToken(anchored, tokens, form);
            if (found != null) return found;
        }
        return null;
    }

    private static @NotNull Token syntheticToken(@NotNull String text, @NotNull List<Token> reference) {
        int line = reference.isEmpty() ? 1 : reference.get(0).line();
        return new Token(TokenKind.IDENT, text, line, 0, text.length());
    }

    private static @NotNull MatchProgress tryCorrectedMatch(@NotNull List<Token> tokens, @NotNull CandidateScore cs, @NotNull TypeRegistry types, @NotNull TypeEnv env) {
        List<Token> corrected = new ArrayList<>(tokens);
        for (int j = 0; j < corrected.size(); j++) {
            if (corrected.get(j) == cs.typoToken) {
                corrected.set(j, syntheticToken(cs.expectedText, tokens));
                break;
            }
        }
        return PatternMatcher.matchWithProgress(corrected, cs.pattern, types, env);
    }

    private static boolean isBuiltin(@NotNull PatternMeta meta) {
        return "Lumen".equals(meta.by());
    }

    private static boolean tryHandlerSandbox(@Nullable Object handler, @NotNull Match match, @NotNull TypeEnv env) {
        if (handler instanceof RegisteredPattern rp) {
            return tryStatementHandler(rp, match, env);
        } else if (handler instanceof RegisteredExpression re) {
            return tryExpressionHandler(re, match, env);
        }
        return true;
    }

    private static @NotNull List<Token> findReorderedFromAnchors(@NotNull List<Token> tokens, @NotNull List<LiteralMatchResult> matchDetails) {
        List<LiteralMatchResult> anchored = matchDetails.stream().filter(m -> m.tokenIndex >= 0).sorted(Comparator.comparingInt(m -> m.literal.partIndex)).toList();
        if (anchored.isEmpty()) return List.of();
        List<Token> result = new ArrayList<>();
        int expectedPos = 0;
        for (LiteralMatchResult m : anchored) {
            if (m.tokenIndex != expectedPos) {
                for (int j = Math.min(expectedPos, m.tokenIndex); j <= Math.max(expectedPos, m.tokenIndex) && j < tokens.size(); j++) {
                    if (!result.contains(tokens.get(j))) result.add(tokens.get(j));
                }
            }
            expectedPos = m.tokenIndex + 1;
        }
        return result.size() >= 2 ? List.copyOf(result) : List.of();
    }

    private static @NotNull String reorderDescription(@NotNull List<Token> reordered) {
        if (reordered.size() == 2) return reordered.get(0).text() + "' and '" + reordered.get(1).text();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reordered.size(); i++) {
            if (i > 0) sb.append(i == reordered.size() - 1 ? "' and '" : "', '");
            sb.append(reordered.get(i).text());
        }
        return sb.toString();
    }

    private static @Nullable CandidateScore scoreAgainst(@NotNull List<Token> tokens, @NotNull Pattern pattern, @NotNull PatternMeta meta, @Nullable Object handler) {
        List<LiteralInfo> literals = extractLiterals(pattern);
        if (literals.isEmpty()) return null;

        boolean[] tokenUsed = new boolean[tokens.size()];
        List<LiteralMatchResult> matches = new ArrayList<>();

        for (LiteralInfo lit : literals) {
            int bestIdx = -1;
            int bestDist = Integer.MAX_VALUE;
            for (int i = 0; i < tokens.size(); i++) {
                if (tokenUsed[i]) continue;
                int dist = FuzzyMatch.distance(tokens.get(i).text(), lit.text);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestIdx = i;
                }
            }
            int threshold = lit.text.length() <= 2 ? 0 : Math.max(1, Math.min(3, (int) (lit.text.length() * 0.4)));
            if (bestIdx >= 0 && bestDist <= threshold) {
                tokenUsed[bestIdx] = true;
                matches.add(new LiteralMatchResult(lit, bestIdx, bestDist));
            } else if (!lit.optional) {
                matches.add(new LiteralMatchResult(lit, -1, bestDist));
            }
        }

        int requiredCount = (int) literals.stream().filter(l -> !l.optional).count();
        if (requiredCount == 0) return null;
        int matchedRequired = (int) matches.stream().filter(m -> m.tokenIndex >= 0 && !m.literal.optional).count();
        if (matchedRequired == 0) return null;

        double matchScore = 0;
        Token typoToken = null;
        String expectedText = null;

        for (LiteralMatchResult m : matches) {
            if (m.tokenIndex < 0) continue;
            if (m.distance == 0) {
                matchScore += 10.0;
            } else {
                matchScore += 5.0;
                if (typoToken == null) {
                    typoToken = tokens.get(m.tokenIndex);
                    expectedText = m.literal.text;
                }
            }
        }

        double matchRatio = matchedRequired / (double) requiredCount;
        double score = matchScore * matchRatio;

        List<Integer> positions = matches.stream().filter(m -> m.tokenIndex >= 0).map(m -> m.tokenIndex).toList();
        boolean inOrder = isOrdered(positions);
        List<Token> reorderedTokens = inOrder ? List.of() : findReorderedTokens(tokens, matches);

        SuggestionKind kind;
        if (typoToken != null) {
            kind = SuggestionKind.TYPO;
        } else if (!inOrder) {
            kind = SuggestionKind.REORDER;
        } else if (matchedRequired == requiredCount) {
            kind = SuggestionKind.TYPE_MISMATCH;
        } else {
            kind = SuggestionKind.CLOSE_MATCH;
        }

        return new CandidateScore(pattern, score, kind, typoToken, expectedText, reorderedTokens, matches, meta, handler);
    }

    private static @NotNull List<Token> findReorderedTokens(@NotNull List<Token> tokens, @NotNull List<LiteralMatchResult> matches) {
        List<LiteralMatchResult> matched = matches.stream().filter(m -> m.tokenIndex >= 0).toList();
        if (matched.size() < 2) return List.of();
        List<Token> result = new ArrayList<>();
        for (int i = 1; i < matched.size(); i++) {
            if (matched.get(i).tokenIndex < matched.get(i - 1).tokenIndex) {
                Token prev = tokens.get(matched.get(i - 1).tokenIndex);
                Token curr = tokens.get(matched.get(i).tokenIndex);
                if (!result.contains(prev)) result.add(prev);
                if (!result.contains(curr)) result.add(curr);
            }
        }
        return List.copyOf(result);
    }

    private static @NotNull List<LiteralInfo> extractLiterals(@NotNull Pattern pattern) {
        List<LiteralInfo> result = new ArrayList<>();
        extractLiteralsFromParts(pattern.parts(), result, false);
        return result;
    }

    private static void extractLiteralsFromParts(@NotNull List<PatternPart> parts, @NotNull List<LiteralInfo> result, boolean parentOptional) {
        int partIndex = result.size();
        for (PatternPart part : parts) {
            if (part instanceof PatternPart.Literal lit) {
                result.add(new LiteralInfo(lit.text(), partIndex++, parentOptional));
            } else if (part instanceof PatternPart.FlexLiteral flex) {
                result.add(new LiteralInfo(flex.forms().get(0), partIndex++, parentOptional));
            } else if (part instanceof PatternPart.Group group) {
                if (!group.alternatives().isEmpty()) {
                    extractLiteralsFromParts(group.alternatives().get(0), result, !group.required() || parentOptional);
                }
                partIndex++;
            } else {
                partIndex++;
            }
        }
    }

    private static boolean isOrdered(@NotNull List<Integer> positions) {
        for (int i = 1; i < positions.size(); i++) {
            if (positions.get(i) <= positions.get(i - 1)) return false;
        }
        return true;
    }

    /**
     * Attempts to run a statement handler in a sandboxed context to verify viability.
     *
     * @param handler the registered statement pattern
     * @param match   the match result from corrected token matching
     * @param env     the type environment
     * @return true if the handler executed without throwing
     */
    public static boolean tryStatementHandler(@NotNull RegisteredPattern handler, @NotNull Match match, @NotNull TypeEnv env) {
        try {
            CodegenContext codegenCtx = new CodegenContext("__simulation__.luma");
            BlockContext blockCtx = new BlockContext(null, null, List.of(), 0);
            BindingContext bc = new BindingContext(match, env, codegenCtx, blockCtx);
            JavaOutput noopOutput = new NoopJavaOutput();
            handler.handler().handle(0, bc, noopOutput);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Attempts to run an expression handler in a sandboxed context to verify viability.
     *
     * @param handler the registered expression pattern
     * @param match   the match result from corrected token matching
     * @param env     the type environment
     * @return true if the handler executed without throwing
     */
    public static boolean tryExpressionHandler(@NotNull RegisteredExpression handler, @NotNull Match match, @NotNull TypeEnv env) {
        try {
            CodegenContext codegenCtx = new CodegenContext("__simulation__.luma");
            BlockContext blockCtx = new BlockContext(null, null, List.of(), 0);
            BindingContext bc = new BindingContext(match, env, codegenCtx, blockCtx);
            handler.handler().handle(bc);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * The kind of mismatch detected between input tokens and a candidate pattern.
     */
    public enum SuggestionKind {
        TYPO,
        REORDER,
        TYPO_AND_REORDER,
        TYPE_MISMATCH,
        CLOSE_MATCH
    }

    /**
     * A single simulation result describing a near match.
     *
     * @param pattern         the candidate pattern that closely matched the input
     * @param kind            what type of mismatch was detected
     * @param score           confidence score (higher is better)
     * @param detail          human readable detail about the mismatch
     * @param errorToken      the specific input token that was wrong (for typos), or null
     * @param expectedText    what the token should have been (for typos), or null
     * @param reorderedTokens input tokens that are out of order relative to the pattern, empty if no reorder
     * @param progress        match progress from real simulation, or null if not enriched
     */
    public record Suggestion(@NotNull Pattern pattern, @NotNull SuggestionKind kind, double score,
                             @Nullable String detail, @Nullable Token errorToken, @Nullable String expectedText,
                             @NotNull List<Token> reorderedTokens, @Nullable MatchProgress progress) {
    }

    private record CandidateScore(@NotNull Pattern pattern, double score, @NotNull SuggestionKind kind,
                                  @Nullable Token typoToken, @Nullable String expectedText,
                                  @NotNull List<Token> reorderedTokens, @NotNull List<LiteralMatchResult> matchDetails,
                                  @NotNull PatternMeta meta, @Nullable Object handler) {
    }

    private record LiteralInfo(@NotNull String text, int partIndex, boolean optional) {
    }

    private record LiteralMatchResult(@NotNull LiteralInfo literal, int tokenIndex, int distance) {
    }

    private static final class NoopJavaOutput implements JavaOutput {

        @Override
        public void line(@NotNull String code) {
        }

        @Override
        public int lineNum() {
            return 0;
        }

        @Override
        public void insertLine(int index, @NotNull String code) {
        }
    }
}
