package dev.lumenlang.lumen.pipeline.language.match;

import dev.lumenlang.lumen.api.exceptions.ParseFailureException;
import dev.lumenlang.lumen.api.util.FuzzyMatch;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.language.TypeBinding;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternPart;
import dev.lumenlang.lumen.pipeline.language.pattern.Placeholder;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.TokenKind;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Matches tokenized input against compiled {@link Pattern} objects.
 *
 * <p>The matcher uses recursive backtracking to handle optional groups,
 * choice groups, flex literals, and typed placeholders. Type bindings
 * determine how many tokens each placeholder consumes.
 *
 * <h2>Backtracking Protection</h2>
 * <p>Calls to {@link TypeBinding#consumeCount} and {@link TypeBinding#parse}
 * are wrapped in defensive error handling. A {@link ParseFailureException}
 * is the normal rejection signal and causes the matcher to try the next
 * candidate.
 */
public final class PatternMatcher {

    private static final Object PARSE_FAILED = new Object();
    private static final int CONSUME_REJECTED = -2;

    private PatternMatcher() {
    }

    /**
     * Attempts to match a list of tokens against a compiled pattern.
     *
     * @param tokens the input tokens to match against
     * @param p      the pattern to match
     * @param types  the type registry for resolving type bindings
     * @param env    the type environment for variable and reference lookups
     * @return a {@link Match} containing bound parameters, or null if matching failed
     */
    public static @Nullable Match match(
            @NotNull List<Token> tokens,
            @NotNull Pattern p,
            @NotNull TypeRegistry types,
            @NotNull TypeEnvImpl env) {
        return match(tokens, p, types, env, null);
    }

    /**
     * Attempts to match a list of tokens against a compiled pattern,
     * with optional InlineExpr support. When a non-null validator is provided,
     * placeholders whose type binding rejects multi-token input may fall back
     * to creating an {@link InlineExpr} if the validator confirms the candidate
     * tokens form a valid expression. When the validator is null, InlineExpr
     * creation is disabled entirely.
     *
     * @param tokens    the input tokens to match against
     * @param p         the pattern to match
     * @param types     the type registry for resolving type bindings
     * @param env       the type environment for variable and reference lookups
     * @param validator validator for InlineExpr candidates, or null to disable InlineExpr
     * @return a {@link Match} containing bound parameters, or null if matching failed
     */
    public static @Nullable Match match(
            @NotNull List<Token> tokens,
            @NotNull Pattern p,
            @NotNull TypeRegistry types,
            @NotNull TypeEnvImpl env,
            @Nullable InlineExprValidator validator) {
        if (LumenLogger.isFullDebug()) {
            LumenLogger.debug("PatternMatcher.match", "Matching pattern: '" + p.raw() + "'");
            LumenLogger.debug("PatternMatcher.match", "Input tokens (" + tokens.size() + "): " + tokens.stream().map(t -> "'" + t.text() + "'").reduce((a, b) -> a + " " + b).orElse(""));
            StringBuilder partsDesc = new StringBuilder();
            for (int i = 0; i < p.parts().size(); i++) {
                if (i > 0) partsDesc.append(", ");
                PatternPart pt = p.parts().get(i);
                if (pt instanceof PatternPart.Literal lit) partsDesc.append("Lit(").append(lit.text()).append(")");
                else if (pt instanceof PatternPart.FlexLiteral fl) partsDesc.append("Flex(").append(fl.forms()).append(")");
                else if (pt instanceof PatternPart.PlaceholderPart pp) partsDesc.append("PH(").append(pp.ph().name()).append(":").append(pp.ph().typeId()).append(")");
                else if (pt instanceof PatternPart.Group g) partsDesc.append("Group(req=").append(g.required()).append(",alts=").append(g.alternatives().size()).append(")");
                else partsDesc.append("UNKNOWN(").append(pt.getClass().getName()).append(")");
            }
            LumenLogger.debug("PatternMatcher.match", "Parts (" + p.parts().size() + "): " + partsDesc);
        }

        Map<String, BoundValue> map = new LinkedHashMap<>();
        List<String> choices = new ArrayList<>();
        int consumed;
        try {
            consumed = tryMatch(tokens, 0, p.parts(), 0, types, env, map, choices, validator, null);
        } catch (Throwable t) {
            return null;
        }

        if (consumed == tokens.size()) return new Match(p, map, List.copyOf(choices));
        return null;
    }

    /**
     * Attempts to match tokens against a pattern, capturing diagnostic
     * information about how far matching progressed before failure.
     *
     * @param tokens the input tokens to match against
     * @param p      the pattern to match
     * @param types  the type registry for resolving type bindings
     * @param env    the type environment for variable and reference lookups
     * @return a {@link MatchProgress} describing the match attempt (check {@link MatchProgress#succeeded()})
     */
    public static @NotNull MatchProgress matchWithProgress(@NotNull List<Token> tokens, @NotNull Pattern p, @NotNull TypeRegistry types, @NotNull TypeEnvImpl env) {
        return matchWithProgress(tokens, p, types, env, null);
    }

    /**
     * Attempts to match tokens against a pattern with optional InlineExpr
     * support, capturing diagnostic information about how far matching
     * progressed before failure.
     *
     * @param tokens    the input tokens to match against
     * @param p         the pattern to match
     * @param types     the type registry for resolving type bindings
     * @param env       the type environment for variable and reference lookups
     * @param validator validator for InlineExpr candidates, or null to disable InlineExpr
     * @return a {@link MatchProgress} describing the match attempt (check {@link MatchProgress#succeeded()})
     */
    public static @NotNull MatchProgress matchWithProgress(@NotNull List<Token> tokens, @NotNull Pattern p, @NotNull TypeRegistry types, @NotNull TypeEnvImpl env, @Nullable InlineExprValidator validator) {
        MatchProgress progress = new MatchProgress();
        Map<String, BoundValue> map = new LinkedHashMap<>();
        List<String> choices = new ArrayList<>();
        int consumed;
        try {
            consumed = tryMatch(tokens, 0, p.parts(), 0, types, env, map, choices, validator, progress);
        } catch (Throwable t) {
            return progress;
        }
        if (consumed == tokens.size()) {
            progress.recordSuccess(new Match(p, map, List.copyOf(choices)));
        } else if (consumed >= 0) {
            progress.recordFailure(consumed, null, null, null, tokens.subList(consumed, tokens.size()));
        }
        return progress;
    }

    private static int tryMatch(
            @NotNull List<Token> tokens,
            int ti,
            @NotNull List<PatternPart> parts,
            int pi,
            @NotNull TypeRegistry types,
            @NotNull TypeEnvImpl env,
            @NotNull Map<String, BoundValue> map,
            @NotNull List<String> choices,
            @Nullable InlineExprValidator validator,
            @Nullable MatchProgress progress) {
        while (pi < parts.size()) {
            PatternPart part = parts.get(pi);

        if (part instanceof PatternPart.Literal lit) {
            if (ti >= tokens.size()) {
                if (progress != null) progress.recordFailure(ti, lit, null, null, List.of());
                return -1;
            }
            int merged = tryMergeTokens(tokens, ti, lit.text());
            if (merged < 0) {
                if (progress != null) progress.recordFailure(ti, lit, null, null, List.of(tokens.get(ti)));
                return -1;
            }
            ti += merged;
            pi++;
            continue;
        }

        if (part instanceof PatternPart.FlexLiteral flex) {
            if (ti >= tokens.size()) {
                if (progress != null) progress.recordFailure(ti, flex, null, null, List.of());
                return -1;
            }
            int bestConsumed = -1;
            for (String form : flex.forms()) {
                int merged = tryMergeTokens(tokens, ti, form);
                if (merged > 0 && merged > bestConsumed) bestConsumed = merged;
            }
            if (bestConsumed < 0) {
                if (progress != null) progress.recordFailure(ti, flex, null, null, List.of(tokens.get(ti)));
                return -1;
            }
            ti += bestConsumed;
            pi++;
            continue;
        }

        if (part instanceof PatternPart.Group group) {
            List<PatternPart> tail = parts.subList(pi + 1, parts.size());
            for (List<PatternPart> alt : group.alternatives()) {
                List<PatternPart> combined = new ArrayList<>(alt.size() + tail.size());
                combined.addAll(alt);
                combined.addAll(tail);
                Map<String, BoundValue> snapshot = new LinkedHashMap<>(map);
                int choicesSnapshot = choices.size();
                if (group.required()) {
                    choices.add(altText(alt));
                }
                int result = tryMatch(tokens, ti, combined, 0, types, env, map, choices, validator, progress);
                if (result >= 0) return result;

                map.clear();
                map.putAll(snapshot);
                while (choices.size() > choicesSnapshot)
                    choices.remove(choices.size() - 1);
            }

            if (!group.required()) {
                return tryMatch(tokens, ti, parts, pi + 1, types, env, map, choices, validator, progress);
            }
            if (progress != null)
                progress.recordFailure(ti, group, null, null, ti < tokens.size() ? List.of(tokens.get(ti)) : List.of());
            return -1;
        }

        if (part instanceof PatternPart.PlaceholderPart pp) {
            Placeholder ph = pp.ph();
            TypeBinding binding = types.get(ph.typeId());
            if (binding == null) {
                if (progress != null)
                    progress.recordFailure(ti, pp, ph.typeId(), "no type binding registered for '" + ph.typeId() + "'", List.of());
                return -1;
            }

            if (ti < tokens.size()
                    && tokens.get(ti).kind() == TokenKind.SYMBOL
                    && tokens.get(ti).text().equals("{")) {
                int braceEnd = findMatchingBrace(tokens, ti);
                if (braceEnd >= 0) {
                    List<Token> inner = tokens.subList(ti + 1, braceEnd);
                    List<Token> fullSlice = tokens.subList(ti, braceEnd + 1);
                    map.put(ph.name(), new BoundValue(ph, fullSlice, new BraceExpr(inner), binding));
                    return tryMatch(tokens, braceEnd + 1, parts, pi + 1, types, env, map, choices, validator, progress);
                }
            }

            List<Token> remaining = tokens.subList(ti, tokens.size());
            ConsumeOutcome consumeOutcome = safeConsumeCount(binding, remaining, env);
            int consumeCount = consumeOutcome.consumeCount();
            String latestReason = consumeOutcome.reason();

            boolean skipGreedy = false;
            if (consumeOutcome.rejected()) {
                if (validator == null) {
                    if (progress == null) return -1;
                    skipGreedy = true;
                }
            }

            if (!skipGreedy && consumeCount >= 0) {
                int end = ti + consumeCount;
                if (end <= tokens.size()) {
                    List<Token> slice = tokens.subList(ti, end);
                    ParseOutcome parsed = safeParse(binding, slice, env);
                    if (!parsed.failed()) {
                        map.put(ph.name(), new BoundValue(ph, slice, parsed.value(), binding));
                        return tryMatch(tokens, end, parts, pi + 1, types, env, map, choices, validator, progress);
                    }
                    latestReason = parsed.reason();
                }
            }

            if (!skipGreedy) {
                String nextLit = findNextRequiredLiteral(parts, pi + 1);
                int maxEnd = tokens.size();
                if (nextLit != null) {
                    int litIdx = findLiteral(tokens, ti, nextLit);
                    if (litIdx >= 0) maxEnd = litIdx;
                }

                boolean allowInlineExpr = validator != null;
                for (int end = maxEnd; end > ti; end--) {
                    List<Token> slice = tokens.subList(ti, end);
                    ParseOutcome parsed = safeParse(binding, slice, env);
                    Object value = parsed.value();
                    if (parsed.failed()) {
                        latestReason = parsed.reason();
                        if (allowInlineExpr && slice.size() > 1 && validator.canResolve(slice, env)) {
                            value = new InlineExpr(List.copyOf(slice));
                        } else {
                            continue;
                        }
                    }

                    Map<String, BoundValue> snapshot = new LinkedHashMap<>(map);
                    int choicesSnapshot = choices.size();
                    map.put(ph.name(), new BoundValue(ph, slice, value, binding));
                    int result = tryMatch(tokens, end, parts, pi + 1, types, env, map, choices, validator, progress);
                    if (result >= 0) return result;

                    map.clear();
                    map.putAll(snapshot);
                    while (choices.size() > choicesSnapshot)
                        choices.remove(choices.size() - 1);
                }
            }
            if (progress != null) {
                List<Token> fTokens = ti < tokens.size() ? List.of(tokens.get(ti)) : List.of();
                int effectiveConsume = consumeCount > 0 ? consumeCount : 1;
                String reason = latestReason != null ? latestReason : "'" + binding.id() + "' rejected the input";
                if (ti < tokens.size()) {
                    progress.recordBindingFailure(ti, binding.id(), reason, fTokens);
                    int cEnd = ti + effectiveConsume;
                    if (cEnd <= tokens.size()) {
                        MatchProgress contProgress = new MatchProgress();
                        Map<String, BoundValue> cSnapshot = new LinkedHashMap<>(map);
                        int cChoices = choices.size();
                        map.put(ph.name(), new BoundValue(ph, tokens.subList(ti, cEnd), PARSE_FAILED, binding));
                        int contResult = tryMatch(tokens, cEnd, parts, pi + 1, types, env, map, choices, validator, contProgress);
                        progress.transferBindingFailures(contProgress);
                        if (contResult >= 0 && contResult < tokens.size() && contResult > progress.furthestTokenIndex()) {
                            progress.recordUnmatchedTrailingTokens(tokens.subList(contResult, tokens.size()));
                        }
                        if (contResult >= 0 && pi + 1 < parts.size()) {
                            progress.recordFailure(contResult, pp, binding.id(), reason, fTokens);
                        } else if (contProgress.furthestTokenIndex() > progress.furthestTokenIndex()) {
                            progress.recordFailure(contProgress.furthestTokenIndex(), pp, binding.id(), reason, fTokens);
                        }
                        map.clear();
                        map.putAll(cSnapshot);
                        while (choices.size() > cChoices) choices.remove(choices.size() - 1);
                    }
                    discoverDownstreamFailures(tokens, ti + effectiveConsume, parts, pi + 1, types, env, progress);
                }
                progress.recordFailure(ti, pp, binding.id(), reason, fTokens);
            }
            return -1;
        }

            if (progress != null) progress.recordFailure(ti, part, null, null, List.of());
        return -1;
        }
        return ti;
    }

    /**
     * Result of a guarded {@link TypeBinding#consumeCount} call.
     *
     * <p>When {@link #rejected()} is true, {@link #reason()} carries the binding authored
     * rejection message. Otherwise {@code reason} is null and {@code consumeCount} is the
     * value returned by the binding.
     *
     * @param consumeCount the number of tokens to consume, or {@link #CONSUME_REJECTED} when rejected
     * @param reason       a binding authored rejection reason when rejected, otherwise null
     */
    private record ConsumeOutcome(int consumeCount, @Nullable String reason) {
        boolean rejected() {
            return consumeCount == CONSUME_REJECTED;
        }
    }

    /**
     * Result of a guarded {@link TypeBinding#parse} call.
     *
     * <p>When {@link #failed()} is true, {@link #reason()} carries the binding authored
     * rejection message. Otherwise {@code reason} is null and {@code value} is the
     * parsed value.
     *
     * @param value  the parsed value, or {@link #PARSE_FAILED} when rejected
     * @param reason a binding authored rejection reason when failed, otherwise null
     */
    private record ParseOutcome(@Nullable Object value, @Nullable String reason) {
        boolean failed() {
            return value == PARSE_FAILED;
        }
    }

    private static @NotNull ConsumeOutcome safeConsumeCount(@NotNull TypeBinding binding, @NotNull List<Token> tokens, @NotNull TypeEnvImpl env) {
        try {
            return new ConsumeOutcome(binding.consumeCount(tokens, env), null);
        } catch (ParseFailureException e) {
            return new ConsumeOutcome(CONSUME_REJECTED, messageOrInternalError(binding, e));
        } catch (RuntimeException e) {
            LumenLogger.warning("Type binding '" + binding.id() + "' threw unexpected " + e.getClass().getSimpleName() + " in consumeCount: " + e.getMessage() + ". Treating as non-match.");
            return new ConsumeOutcome(CONSUME_REJECTED, internalError(binding, e));
        }
    }

    private static @NotNull ParseOutcome safeParse(@NotNull TypeBinding binding, @NotNull List<Token> tokens, @NotNull TypeEnvImpl env) {
        try {
            return new ParseOutcome(binding.parse(tokens, env), null);
        } catch (ParseFailureException e) {
            LumenLogger.debug("PatternMatcher.match", "  parse threw: " + e.getMessage());
            return new ParseOutcome(PARSE_FAILED, messageOrInternalError(binding, e));
        } catch (RuntimeException e) {
            LumenLogger.warning("Type binding '" + binding.id() + "' threw unexpected " + e.getClass().getSimpleName() + " in parse: " + e.getMessage() + ". Treating as non-match.");
            return new ParseOutcome(PARSE_FAILED, internalError(binding, e));
        }
    }

    private static @NotNull String messageOrInternalError(@NotNull TypeBinding binding, @NotNull RuntimeException e) {
        String msg = e.getMessage();
        return msg != null ? msg : internalError(binding, e);
    }

    private static @NotNull String internalError(@NotNull TypeBinding binding, @NotNull RuntimeException e) {
        return "internal error in '" + binding.id() + "' (" + e.getClass().getSimpleName() + ")";
    }

    private static @NotNull String altText(@NotNull List<PatternPart> alt) {
        StringBuilder sb = new StringBuilder();
        for (PatternPart p : alt) {
            if (p instanceof PatternPart.Literal lit) {
                if (!sb.isEmpty())
                    sb.append(' ');
                sb.append(lit.text());
            } else if (p instanceof PatternPart.FlexLiteral flex) {
                if (!sb.isEmpty())
                    sb.append(' ');
                sb.append(flex.forms().get(0));
            }
        }
        return sb.toString();
    }

    private static @Nullable String findNextRequiredLiteral(@NotNull List<PatternPart> parts, int from) {
        for (int i = from; i < parts.size(); i++) {
            PatternPart p = parts.get(i);
            if (p instanceof PatternPart.Literal lit)
                return lit.text();
            if (p instanceof PatternPart.FlexLiteral flex)
                return flex.forms().get(0);
            if (p instanceof PatternPart.Group g) {
                if (!g.required()) continue;
                for (List<PatternPart> alt : g.alternatives()) {
                    if (!alt.isEmpty() && alt.get(0) instanceof PatternPart.Literal lit)
                        return lit.text();
                    if (!alt.isEmpty() && alt.get(0) instanceof PatternPart.FlexLiteral flex)
                        return flex.forms().get(0);
                }
                return null;
            }
            if (p instanceof PatternPart.PlaceholderPart)
                continue;
            return null;
        }
        return null;
    }

    private static int findLiteral(@NotNull List<Token> tokens, int start, @NotNull String literal) {
        for (int i = start; i < tokens.size(); i++) {
            if (tokens.get(i).text().equalsIgnoreCase(literal))
                return i;
        }
        return -1;
    }

    private static int findMatchingBrace(@NotNull List<Token> tokens, int openIndex) {
        int depth = 1;
        for (int i = openIndex + 1; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.kind() == TokenKind.SYMBOL) {
                if (t.text().equals("{")) depth++;
                else if (t.text().equals("}")) {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }

    private static void discoverDownstreamFailures(@NotNull List<Token> tokens, int ti, @NotNull List<PatternPart> parts, int pi, @NotNull TypeRegistry types, @NotNull TypeEnvImpl env, @NotNull MatchProgress progress) {
        for (int p = pi; p < parts.size(); p++) {
            PatternPart part = parts.get(p);
            if (part instanceof PatternPart.Literal lit) {
                if (ti < tokens.size()) {
                    int merged = tryMergeTokens(tokens, ti, lit.text());
                    if (merged > 0) {
                        ti += merged;
                    } else if (isLiteralTypo(tokens.get(ti).text(), lit.text())) {
                        progress.recordLiteralTypo(tokens.get(ti), lit.text());
                        ti++;
                    }
                }
            } else if (part instanceof PatternPart.FlexLiteral flex) {
                if (ti < tokens.size()) {
                    int bestConsumed = -1;
                    for (String form : flex.forms()) {
                        int merged = tryMergeTokens(tokens, ti, form);
                        if (merged > 0 && merged > bestConsumed) bestConsumed = merged;
                    }
                    if (bestConsumed > 0) {
                        ti += bestConsumed;
                    } else {
                        String tokenText = tokens.get(ti).text();
                        String matchedForm = null;
                        for (String f : flex.forms()) {
                            if (isLiteralTypo(tokenText, f)) {
                                matchedForm = f;
                                break;
                            }
                        }
                        if (matchedForm != null) {
                            progress.recordLiteralTypo(tokens.get(ti), matchedForm);
                            ti++;
                        }
                    }
                }
            } else if (part instanceof PatternPart.PlaceholderPart pp) {
                Placeholder ph = pp.ph();
                TypeBinding binding = types.get(ph.typeId());
                if (binding == null) continue;
                List<Token> remaining = tokens.subList(ti, tokens.size());
                ConsumeOutcome consume = safeConsumeCount(binding, remaining, env);
                if (consume.rejected()) {
                    List<Token> failed = remaining.isEmpty() ? List.of() : List.of(tokens.get(ti));
                    progress.recordBindingFailure(ti, binding.id(), consume.reason(), failed);
                    if (!remaining.isEmpty()) ti++;
                    continue;
                }
                int cc = consume.consumeCount();
                int end = cc < 0 ? tokens.size() : Math.min(ti + cc, tokens.size());
                List<Token> slice = tokens.subList(ti, end);
                ParseOutcome parsed = safeParse(binding, slice, env);
                if (parsed.failed()) {
                    List<Token> failed = slice.isEmpty() ? List.of() : List.of(slice.get(0));
                    progress.recordBindingFailure(ti, binding.id(), parsed.reason(), failed);
                }
                ti = Math.max(end, ti + 1);
            } else if (part instanceof PatternPart.Group group) {
                if (ti < tokens.size()) {
                    for (List<PatternPart> alt : group.alternatives()) {
                        if (!alt.isEmpty() && alt.get(0) instanceof PatternPart.Literal gl && tokens.get(ti).text().equalsIgnoreCase(gl.text())) {
                            ti++;
                            break;
                        }
                        if (!alt.isEmpty() && alt.get(0) instanceof PatternPart.FlexLiteral fl && fl.forms().contains(tokens.get(ti).text().toLowerCase(Locale.ROOT))) {
                            ti++;
                            break;
                        }
                    }
                }
            }
        }
    }

    private static boolean isLiteralTypo(@NotNull String tokenText, @NotNull String literalText) {
        int threshold = Math.max(1, Math.min(3, (int) (Math.min(tokenText.length(), literalText.length()) * 0.4)));
        if (tokenText.length() <= 2 || literalText.length() <= 2) return false;
        return FuzzyMatch.prefixAwareDistance(tokenText, literalText) <= threshold;
    }

    /**
     * Tries to match a literal string against one or more adjacent tokens that form a single word
     * (no whitespace gap between consecutive tokens). Returns the number of tokens consumed, or -1
     * if no match.
     */
    private static int tryMergeTokens(@NotNull List<Token> tokens, int start, @NotNull String literal) {
        if (tokens.get(start).text().equalsIgnoreCase(literal)) return 1;
        StringBuilder merged = new StringBuilder(tokens.get(start).text());
        for (int i = start + 1; i < tokens.size(); i++) {
            Token prev = tokens.get(i - 1);
            Token curr = tokens.get(i);
            if (curr.start() != prev.end()) break;
            merged.append(curr.text());
            if (merged.toString().equalsIgnoreCase(literal)) return i - start + 1;
            if (merged.length() > literal.length()) break;
        }
        return -1;
    }
}
