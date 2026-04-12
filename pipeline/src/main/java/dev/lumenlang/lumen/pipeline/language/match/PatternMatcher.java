package dev.lumenlang.lumen.pipeline.language.match;

import dev.lumenlang.lumen.api.exceptions.ParseFailureException;
import dev.lumenlang.lumen.api.util.FuzzyMatch;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
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
            @NotNull TypeEnv env) {
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
            @NotNull TypeEnv env,
            @Nullable InlineExprValidator validator) {
        LumenLogger.debug("PatternMatcher.match", "Matching pattern: '" + p.raw() + "'");
        LumenLogger.debug("PatternMatcher.match", "Input tokens (" + tokens.size() + "): " +
                tokens.stream().map(t -> "'" + t.text() + "'").reduce((a, b) -> a + " " + b).orElse(""));

        StringBuilder partsDesc = new StringBuilder();
        for (int i = 0; i < p.parts().size(); i++) {
            if (i > 0)
                partsDesc.append(", ");
            PatternPart pt = p.parts().get(i);
            if (pt instanceof PatternPart.Literal lit)
                partsDesc.append("Lit(").append(lit.text()).append(")");
            else if (pt instanceof PatternPart.FlexLiteral fl)
                partsDesc.append("Flex(").append(fl.forms()).append(")");
            else if (pt instanceof PatternPart.PlaceholderPart pp)
                partsDesc.append("PH(").append(pp.ph().name()).append(":").append(pp.ph().typeId()).append(")");
            else if (pt instanceof PatternPart.Group g)
                partsDesc.append("Group(req=").append(g.required()).append(",alts=").append(g.alternatives().size())
                        .append(")");
            else
                partsDesc.append("UNKNOWN(").append(pt.getClass().getName()).append(")");
        }
        LumenLogger.debug("PatternMatcher.match", "Parts (" + p.parts().size() + "): " + partsDesc);

        Map<String, BoundValue> map = new LinkedHashMap<>();
        List<String> choices = new ArrayList<>();
        int consumed;
        try {
            consumed = tryMatch(tokens, 0, p.parts(), 0, types, env, map, choices, validator, null);
        } catch (Throwable t) {
            LumenLogger.debug("PatternMatcher.match",
                    "EXCEPTION in tryMatch: " + t.getClass().getName() + ": " + t.getMessage());
            return null;
        }

        if (consumed == tokens.size()) {
            LumenLogger.debug("PatternMatcher.match", "SUCCESS: Pattern matched completely");
            return new Match(p, map, List.copyOf(choices));
        }

        LumenLogger.debug("PatternMatcher.match", "FAILED: " +
                (consumed < 0 ? "match failed" : "unmatched tokens remaining (" + (tokens.size() - consumed) + ")"));
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
    public static @NotNull MatchProgress matchWithProgress(@NotNull List<Token> tokens, @NotNull Pattern p, @NotNull TypeRegistry types, @NotNull TypeEnv env) {
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
    public static @NotNull MatchProgress matchWithProgress(@NotNull List<Token> tokens, @NotNull Pattern p, @NotNull TypeRegistry types, @NotNull TypeEnv env, @Nullable InlineExprValidator validator) {
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
            progress.recordFailure(consumed, null, null, tokens.subList(consumed, tokens.size()));
        }
        return progress;
    }

    private static int tryMatch(
            @NotNull List<Token> tokens,
            int ti,
            @NotNull List<PatternPart> parts,
            int pi,
            @NotNull TypeRegistry types,
            @NotNull TypeEnv env,
            @NotNull Map<String, BoundValue> map,
            @NotNull List<String> choices,
            @Nullable InlineExprValidator validator,
            @Nullable MatchProgress progress) {
        if (pi >= parts.size())
            return ti;

        PatternPart part = parts.get(pi);

        if (part instanceof PatternPart.Literal lit) {
            if (ti >= tokens.size()) {
                if (progress != null) progress.recordFailure(ti, lit, null, List.of());
                return -1;
            }
            if (!tokens.get(ti).text().equalsIgnoreCase(lit.text())) {
                if (progress != null) progress.recordFailure(ti, lit, null, List.of(tokens.get(ti)));
                return -1;
            }
            return tryMatch(tokens, ti + 1, parts, pi + 1, types, env, map, choices, validator, progress);
        }

        if (part instanceof PatternPart.FlexLiteral flex) {
            if (ti >= tokens.size()) {
                if (progress != null) progress.recordFailure(ti, flex, null, List.of());
                return -1;
            }
            String tok = tokens.get(ti).text().toLowerCase(Locale.ROOT);
            boolean matched = false;
            for (String form : flex.forms()) {
                if (form.equals(tok)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                if (progress != null) progress.recordFailure(ti, flex, null, List.of(tokens.get(ti)));
                return -1;
            }
            return tryMatch(tokens, ti + 1, parts, pi + 1, types, env, map, choices, validator, progress);
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
                if (result >= 0)
                    return result;

                map.clear();
                map.putAll(snapshot);
                while (choices.size() > choicesSnapshot)
                    choices.remove(choices.size() - 1);
            }

            if (!group.required()) {
                return tryMatch(tokens, ti, parts, pi + 1, types, env, map, choices, validator, progress);
            }
            if (progress != null)
                progress.recordFailure(ti, group, null, ti < tokens.size() ? List.of(tokens.get(ti)) : List.of());
            return -1;
        }

        if (part instanceof PatternPart.PlaceholderPart pp) {
            Placeholder ph = pp.ph();
            TypeBinding binding = types.get(ph.typeId());
            if (binding == null) {
                LumenLogger.debug("PatternMatcher.match", "FAILED: No type binding for '" + ph.typeId() + "'");
                if (progress != null) progress.recordFailure(ti, pp, ph.typeId(), List.of());
                return -1;
            }

            if (ti < tokens.size()
                    && tokens.get(ti).kind() == TokenKind.SYMBOL
                    && tokens.get(ti).text().equals("{")) {
                int braceEnd = findMatchingBrace(tokens, ti);
                if (braceEnd >= 0) {
                    List<Token> inner = tokens.subList(ti + 1, braceEnd);
                    List<Token> fullSlice = tokens.subList(ti, braceEnd + 1);
                    LumenLogger.debug("PatternMatcher.match",
                            "Brace group for %" + ph.name() + ":" + ph.typeId()
                                    + "% consuming tokens " + ti + ".." + braceEnd);
                    map.put(ph.name(), new BoundValue(ph, fullSlice, new BraceExpr(inner), binding));
                    return tryMatch(tokens, braceEnd + 1, parts, pi + 1, types, env, map, choices, validator, progress);
                }
            }

            List<Token> remaining = tokens.subList(ti, tokens.size());
            LumenLogger.debug("PatternMatcher.match", "Placeholder %" + ph.name() + ":" + ph.typeId()
                    + "% at ti=" + ti + " remaining=" + remaining.stream()
                    .map(t -> "'" + t.text() + "'").reduce((a, b) -> a + " " + b).orElse("(empty)"));

            int consumeCount = safeConsumeCount(binding, remaining, env, progress);
            LumenLogger.debug("PatternMatcher.match", "  consumeCount=" + consumeCount);

            if (consumeCount == CONSUME_REJECTED) {
                if (validator == null) {
                    if (progress != null)
                        progress.recordFailure(ti, pp, binding.id(), ti < tokens.size() ? List.of(tokens.get(ti)) : List.of());
                    return -1;
                }
                LumenLogger.debug("PatternMatcher.match", "  consumeCount rejected but validator present, falling through to inline backtracking");
            }

            if (consumeCount >= 0) {
                int end = ti + consumeCount;
                if (end <= tokens.size()) {
                    List<Token> slice = tokens.subList(ti, end);
                    Object value = safeParse(binding, slice, env, progress);
                    if (value != PARSE_FAILED) {
                        LumenLogger.debug("PatternMatcher.match", "  parse OK, value=" + value);
                        map.put(ph.name(), new BoundValue(ph, slice, value, binding));
                        return tryMatch(tokens, end, parts, pi + 1, types, env, map, choices, validator, progress);
                    }
                }
                LumenLogger.debug("PatternMatcher.match",
                        "  fixed consumeCount failed, falling through to greedy backtracking");
            }

            String nextLit = findNextRequiredLiteral(parts, pi + 1);
            int maxEnd = tokens.size();
            if (nextLit != null) {
                int litIdx = findLiteral(tokens, ti, nextLit);
                if (litIdx >= 0)
                    maxEnd = litIdx;
            }

            boolean allowInlineExpr = validator != null;
            for (int end = maxEnd; end > ti; end--) {
                List<Token> slice = tokens.subList(ti, end);
                Object value = safeParse(binding, slice, env, progress);
                if (value == PARSE_FAILED) {
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
                if (result >= 0)
                    return result;

                map.clear();
                map.putAll(snapshot);
                while (choices.size() > choicesSnapshot)
                    choices.remove(choices.size() - 1);
            }
            if (progress != null) {
                List<Token> fTokens = ti < tokens.size() ? List.of(tokens.get(ti)) : List.of();
                if (consumeCount > 0) {
                    progress.recordBindingFailure(ti, binding.id(), fTokens);
                    int cEnd = ti + consumeCount;
                    if (cEnd <= tokens.size()) {
                        MatchProgress contProgress = new MatchProgress();
                        Map<String, BoundValue> cSnapshot = new LinkedHashMap<>(map);
                        int cChoices = choices.size();
                        map.put(ph.name(), new BoundValue(ph, tokens.subList(ti, cEnd), PARSE_FAILED, binding));
                        tryMatch(tokens, cEnd, parts, pi + 1, types, env, map, choices, validator, contProgress);
                        progress.transferBindingFailures(contProgress);
                        map.clear();
                        map.putAll(cSnapshot);
                        while (choices.size() > cChoices) choices.remove(choices.size() - 1);
                    }
                    discoverDownstreamFailures(tokens, ti + consumeCount, parts, pi + 1, types, env, progress);
                }
                progress.recordFailure(ti, pp, binding.id(), fTokens);
            }
            return -1;
        }

        if (progress != null) progress.recordFailure(ti, part, null, List.of());
        return -1;
    }

    /**
     * Safely calls {@link TypeBinding#consumeCount}.
     */
    private static int safeConsumeCount(
            @NotNull TypeBinding binding,
            @NotNull List<Token> tokens,
            @NotNull TypeEnv env,
            @Nullable MatchProgress progress) {
        try {
            return binding.consumeCount(tokens, env);
        } catch (ParseFailureException e) {
            LumenLogger.debug("PatternMatcher.match", "  consumeCount threw: " + e.getMessage());
            if (progress != null) progress.storeRejectionReason(e.getMessage());
            return CONSUME_REJECTED;
        } catch (RuntimeException e) {
            LumenLogger.warning("Type binding '" + binding.id()
                    + "' threw unexpected " + e.getClass().getSimpleName()
                    + " in consumeCount: " + e.getMessage()
                    + ". Treating as non-match.");
            return CONSUME_REJECTED;
        }
    }

    /**
     * Safely calls {@link TypeBinding#parse}.
     */
    private static @Nullable Object safeParse(
            @NotNull TypeBinding binding,
            @NotNull List<Token> tokens,
            @NotNull TypeEnv env,
            @Nullable MatchProgress progress) {
        try {
            return binding.parse(tokens, env);
        } catch (ParseFailureException e) {
            LumenLogger.debug("PatternMatcher.match", "  parse threw: " + e.getMessage());
            if (progress != null) progress.storeRejectionReason(e.getMessage());
            return PARSE_FAILED;
        } catch (RuntimeException e) {
            LumenLogger.warning("Type binding '" + binding.id()
                    + "' threw unexpected " + e.getClass().getSimpleName()
                    + " in parse: " + e.getMessage()
                    + ". Treating as non-match.");
            return PARSE_FAILED;
        }
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

    private static void discoverDownstreamFailures(@NotNull List<Token> tokens, int ti, @NotNull List<PatternPart> parts, int pi, @NotNull TypeRegistry types, @NotNull TypeEnv env, @NotNull MatchProgress progress) {
        for (int p = pi; p < parts.size(); p++) {
            PatternPart part = parts.get(p);
            if (part instanceof PatternPart.Literal lit) {
                if (ti < tokens.size()) {
                    if (tokens.get(ti).text().equalsIgnoreCase(lit.text())) {
                        ti++;
                    } else if (isLiteralTypo(tokens.get(ti).text(), lit.text())) {
                        progress.recordLiteralTypo(tokens.get(ti), lit.text());
                        ti++;
                    }
                }
            } else if (part instanceof PatternPart.FlexLiteral flex) {
                if (ti < tokens.size()) {
                    String lower = tokens.get(ti).text().toLowerCase(Locale.ROOT);
                    if (flex.forms().contains(lower)) {
                        ti++;
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
                if (remaining.isEmpty()) {
                    progress.clearRejectionReason();
                    progress.recordBindingFailure(ti, binding.id(), List.of());
                    continue;
                }
                MatchProgress scratch = new MatchProgress();
                int cc = safeConsumeCount(binding, remaining, env, scratch);
                if (cc < 0) {
                    progress.clearRejectionReason();
                    if (scratch.failedReason() == null) progress.storeRejectionReason(binding.id() + " rejected input");
                    progress.recordBindingFailure(ti, binding.id(), List.of(tokens.get(ti)));
                    ti++;
                    continue;
                }
                int end = ti + cc;
                if (end <= tokens.size()) {
                    List<Token> slice = tokens.subList(ti, end);
                    Object val = safeParse(binding, slice, env, scratch);
                    if (val == PARSE_FAILED) {
                        progress.clearRejectionReason();
                        progress.recordBindingFailure(ti, binding.id(), List.of(tokens.get(ti)));
                    }
                    ti = end;
                } else {
                    progress.clearRejectionReason();
                    progress.recordBindingFailure(ti, binding.id(), List.of(tokens.get(ti)));
                    ti++;
                }
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
}
