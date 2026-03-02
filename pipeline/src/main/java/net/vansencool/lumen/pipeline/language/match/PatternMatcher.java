package net.vansencool.lumen.pipeline.language.match;

import net.vansencool.lumen.api.exceptions.ParseFailureException;
import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.language.TypeBinding;
import net.vansencool.lumen.pipeline.language.pattern.Pattern;
import net.vansencool.lumen.pipeline.language.pattern.PatternPart;
import net.vansencool.lumen.pipeline.language.pattern.Placeholder;
import net.vansencool.lumen.pipeline.language.tokenization.Token;
import net.vansencool.lumen.pipeline.language.tokenization.TokenKind;
import net.vansencool.lumen.pipeline.logger.LumenLogger;
import net.vansencool.lumen.pipeline.typebinding.TypeRegistry;
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
 * candidate. Any other {@link RuntimeException} (typically from a buggy addon
 * type binding) is caught, logged as a warning, and treated as a non-match
 * rather than crashing the entire compilation pipeline.
 */
public final class PatternMatcher {

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
            consumed = tryMatch(tokens, 0, p.parts(), 0, types, env, map, choices, validator);
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

    private static int tryMatch(
            @NotNull List<Token> tokens,
            int ti,
            @NotNull List<PatternPart> parts,
            int pi,
            @NotNull TypeRegistry types,
            @NotNull TypeEnv env,
            @NotNull Map<String, BoundValue> map,
            @NotNull List<String> choices,
            @Nullable InlineExprValidator validator) {
        if (pi >= parts.size())
            return ti;

        PatternPart part = parts.get(pi);

        if (part instanceof PatternPart.Literal lit) {
            if (ti >= tokens.size())
                return -1;
            if (!tokens.get(ti).text().equalsIgnoreCase(lit.text()))
                return -1;
            return tryMatch(tokens, ti + 1, parts, pi + 1, types, env, map, choices, validator);
        }

        if (part instanceof PatternPart.FlexLiteral flex) {
            if (ti >= tokens.size())
                return -1;
            String tok = tokens.get(ti).text().toLowerCase(Locale.ROOT);
            boolean matched = false;
            for (String form : flex.forms()) {
                if (form.equals(tok)) {
                    matched = true;
                    break;
                }
            }
            if (!matched)
                return -1;
            return tryMatch(tokens, ti + 1, parts, pi + 1, types, env, map, choices, validator);
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
                int result = tryMatch(tokens, ti, combined, 0, types, env, map, choices, validator);
                if (result >= 0)
                    return result;

                map.clear();
                map.putAll(snapshot);
                while (choices.size() > choicesSnapshot)
                    choices.remove(choices.size() - 1);
            }

            if (!group.required()) {
                return tryMatch(tokens, ti, parts, pi + 1, types, env, map, choices, validator);
            }
            return -1;
        }

        if (part instanceof PatternPart.PlaceholderPart pp) {
            Placeholder ph = pp.ph();
            TypeBinding binding = types.get(ph.typeId());
            if (binding == null) {
                LumenLogger.debug("PatternMatcher.match", "FAILED: No type binding for '" + ph.typeId() + "'");
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
                    return tryMatch(tokens, braceEnd + 1, parts, pi + 1, types, env, map, choices, validator);
                }
            }

            List<Token> remaining = tokens.subList(ti, tokens.size());
            LumenLogger.debug("PatternMatcher.match", "Placeholder %" + ph.name() + ":" + ph.typeId()
                    + "% at ti=" + ti + " remaining=" + remaining.stream()
                            .map(t -> "'" + t.text() + "'").reduce((a, b) -> a + " " + b).orElse("(empty)"));

            int consumeCount = safeConsumeCount(binding, remaining, env);
            LumenLogger.debug("PatternMatcher.match", "  consumeCount=" + consumeCount);

            if (consumeCount == CONSUME_REJECTED) {
                return -1;
            }

            if (consumeCount >= 0) {
                int end = ti + consumeCount;
                if (end <= tokens.size()) {
                    List<Token> slice = tokens.subList(ti, end);
                    Object value = safeParse(binding, slice, env);
                    if (value != PARSE_FAILED) {
                        LumenLogger.debug("PatternMatcher.match", "  parse OK, value=" + value);
                        map.put(ph.name(), new BoundValue(ph, slice, value, binding));
                        return tryMatch(tokens, end, parts, pi + 1, types, env, map, choices, validator);
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

            boolean allowInlineExpr = validator != null && consumeCount >= 0;
            for (int end = maxEnd; end > ti; end--) {
                List<Token> slice = tokens.subList(ti, end);
                Object value = safeParse(binding, slice, env);
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
                int result = tryMatch(tokens, end, parts, pi + 1, types, env, map, choices, validator);
                if (result >= 0)
                    return result;

                map.clear();
                map.putAll(snapshot);
                while (choices.size() > choicesSnapshot)
                    choices.remove(choices.size() - 1);
            }
            return -1;
        }

        return -1;
    }

    private static final Object PARSE_FAILED = new Object();

    private static final int CONSUME_REJECTED = -2;

    /**
     * Safely calls {@link TypeBinding#consumeCount}.
     */
    private static int safeConsumeCount(
            @NotNull TypeBinding binding,
            @NotNull List<Token> tokens,
            @NotNull TypeEnv env) {
        try {
            return binding.consumeCount(tokens, env);
        } catch (ParseFailureException e) {
            LumenLogger.debug("PatternMatcher.match", "  consumeCount threw: " + e.getMessage());
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
            @NotNull TypeEnv env) {
        try {
            return binding.parse(tokens, env);
        } catch (ParseFailureException e) {
            LumenLogger.debug("PatternMatcher.match", "  parse threw: " + e.getMessage());
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
            if (p instanceof PatternPart.Group g && !g.required())
                continue;
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
}
