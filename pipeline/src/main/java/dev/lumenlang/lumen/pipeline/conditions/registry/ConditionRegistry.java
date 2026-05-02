package dev.lumenlang.lumen.pipeline.conditions.registry;

import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.pattern.PatternMeta;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.language.compile.PatternCompiler;
import dev.lumenlang.lumen.pipeline.language.match.InlineExprValidator;
import dev.lumenlang.lumen.pipeline.language.match.Match;
import dev.lumenlang.lumen.pipeline.language.match.PatternMatcher;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternIndex;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternPart;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registry for all condition patterns available in a Lumen script.
 *
 * <p>Condition patterns are matched against the token stream produced from the Boolean expression
 * portion of an {@code if} or {@code else if} line. Patterns are tried in registration order;
 * the first match wins.
 *
 * <p>This registry is owned by the {@link PatternRegistry} and
 * should not be instantiated independently.
 *
 * @see RegisteredCondition
 * @see PatternRegistry#condition(String, ConditionHandler)
 */
public class ConditionRegistry {

    private final List<RegisteredCondition> conditions = new ArrayList<>();
    private final TypeRegistry types;
    private volatile PatternIndex<RegisteredCondition> conditionIndex;
    private volatile @Nullable Map<String, KeywordAbsorption> splitProtectedKeywords;
    private @Nullable InlineExprValidator validator;

    /**
     * Describes how many top-level connector keywords (and/or) are absorbed by a protected
     * keyword's surrounding pattern.
     *
     * @param absorbsAnd how many subsequent {@code and} tokens belong to the pattern
     * @param absorbsOr  how many subsequent {@code or} tokens belong to the pattern
     */
    public record KeywordAbsorption(int absorbsAnd, int absorbsOr) {}

    /**
     * Creates a new registry backed by the given type registry.
     *
     * @param types the type registry used during pattern matching
     */
    public ConditionRegistry(TypeRegistry types) {
        this.types = types;
    }

    /**
     * Sets the InlineExpr validator used during the slow retry path.
     *
     * @param validator the validator to use, or null to disable InlineExpr entirely
     */
    public void setInlineExprValidator(@Nullable InlineExprValidator validator) {
        this.validator = validator;
    }

    /**
     * Registers a condition pattern with its handler, without documentation metadata.
     *
     * @param pattern the raw pattern string (e.g. {@code "%p:PLAYER% has permission %perm:STRING%"})
     * @param handler the handler that generates the Java boolean expression
     */
    public void register(@NotNull String pattern, @NotNull ConditionHandler handler) {
        register(pattern, handler, PatternMeta.EMPTY);
    }

    /**
     * Registers a condition pattern with its handler and documentation metadata.
     *
     * @param pattern the raw pattern string
     * @param handler the handler that generates the Java boolean expression
     * @param meta    documentation metadata for this condition
     */
    public void register(@NotNull String pattern, @NotNull ConditionHandler handler, @NotNull PatternMeta meta) {
        Pattern compiled = PatternCompiler.compile(pattern);
        validateTypes(compiled);
        conditions.add(new RegisteredCondition(compiled, handler, meta));
        conditionIndex = null;
        splitProtectedKeywords = null;
    }

    private void validateTypes(@NotNull Pattern pattern) {
        List<String> missing = new ArrayList<>();
        collectMissingTypes(pattern.parts(), missing);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Condition pattern '" + pattern.raw()
                    + "' references unknown type binding(s): " + String.join(", ", missing));
        }
    }

    private void collectMissingTypes(@NotNull List<PatternPart> parts, @NotNull List<String> missing) {
        for (PatternPart part : parts) {
            if (part instanceof PatternPart.PlaceholderPart pp) {
                String typeId = pp.ph().typeId();
                if (types.get(typeId) == null && !missing.contains(typeId)) {
                    missing.add(typeId);
                }
            } else if (part instanceof PatternPart.Group group) {
                for (List<PatternPart> alt : group.alternatives()) {
                    collectMissingTypes(alt, missing);
                }
            }
        }
    }

    /**
     * Returns an unmodifiable view of all registered conditions.
     *
     * @return the list of registered conditions
     */
    public @NotNull List<RegisteredCondition> getConditions() {
        return List.copyOf(conditions);
    }

    /**
     * Attempts to match the given tokens against all registered condition patterns.
     *
     * <p>Patterns are sorted by specificity (more literals = higher priority) and
     * tried in order. The first successful match is returned.
     *
     * @param tokens the condition token list to match
     * @param env    the current type environment
     * @return a {@link RegisteredConditionMatch} on success, or {@code null} if no pattern matched
     */
    public RegisteredConditionMatch match(List<Token> tokens, TypeEnvImpl env) {
        for (RegisteredCondition rc : ensureIndex().candidates(tokens)) {
            Match m = PatternMatcher.match(tokens, rc.pattern(), types, env);
            if (m != null) return new RegisteredConditionMatch(rc, m);
        }
        return null;
    }

    /**
     * Slow path for condition matching that supports inline expressions.
     * Should only be called when the fast path ({@link #match}) returned null.
     *
     * @param tokens the condition token list to match
     * @param env    the current type environment
     * @return a {@link RegisteredConditionMatch} on success, or {@code null} if no pattern matched
     */
    public @Nullable RegisteredConditionMatch matchSlow(@NotNull List<Token> tokens, @NotNull TypeEnvImpl env) {
        if (validator == null) return null;
        for (RegisteredCondition rc : ensureIndex().candidates(tokens)) {
            Match m = PatternMatcher.match(tokens, rc.pattern(), types, env, validator);
            if (m != null) return new RegisteredConditionMatch(rc, m);
        }
        return null;
    }

    /**
     * Returns a map of literal keywords that, when present in a condition's input tokens,
     * absorb a number of subsequent top-level {@code and}/{@code or} tokens.
     *
     * <p>Computed by scanning every registered condition pattern. For each pattern whose
     * literal sequence contains an {@code and} or {@code or}, the literals appearing
     * BEFORE the connector are recorded as protected markers, mapped to the count of
     * connectors of each kind that follow them inside the same pattern.
     *
     * <p>Example: {@code is between %min% and %max%} registers {@code between} with
     * {@code absorbsAnd=1}. When the parser sees {@code between} in the input, it knows
     * to skip splitting on the next top-level {@code and}.
     */
    public @NotNull Map<String, KeywordAbsorption> splitProtectedKeywords() {
        Map<String, KeywordAbsorption> cached = splitProtectedKeywords;
        if (cached != null) return cached;
        synchronized (conditions) {
            cached = splitProtectedKeywords;
            if (cached != null) return cached;
            Map<String, KeywordAbsorption> built = new HashMap<>();
            for (RegisteredCondition rc : conditions) {
                List<String> literals = collectLiterals(rc.pattern().parts());
                int firstConnector = -1;
                for (int i = 0; i < literals.size(); i++) {
                    String l = literals.get(i);
                    if (l.equals("and") || l.equals("or")) {
                        firstConnector = i;
                        break;
                    }
                }
                if (firstConnector <= 0) continue;
                int andCount = 0;
                int orCount = 0;
                for (int i = firstConnector; i < literals.size(); i++) {
                    String l = literals.get(i);
                    if (l.equals("and")) andCount++;
                    else if (l.equals("or")) orCount++;
                }
                String marker = literals.get(firstConnector - 1);
                if (marker.equals("and") || marker.equals("or")) continue;
                KeywordAbsorption existing = built.get(marker);
                int a = Math.max(existing == null ? 0 : existing.absorbsAnd(), andCount);
                int o = Math.max(existing == null ? 0 : existing.absorbsOr(), orCount);
                built.put(marker, new KeywordAbsorption(a, o));
            }
            splitProtectedKeywords = Map.copyOf(built);
            return splitProtectedKeywords;
        }
    }

    private static @NotNull List<String> collectLiterals(@NotNull List<PatternPart> parts) {
        List<String> out = new ArrayList<>();
        for (PatternPart p : parts) {
            if (p instanceof PatternPart.Literal lit) {
                out.add(lit.text().toLowerCase(Locale.ROOT));
            } else if (p instanceof PatternPart.FlexLiteral flex) {
                for (String form : flex.forms()) out.add(form.toLowerCase(Locale.ROOT));
            } else if (p instanceof PatternPart.Group g) {
                for (List<PatternPart> alt : g.alternatives()) out.addAll(collectLiterals(alt));
            }
        }
        return out;
    }

    private @NotNull PatternIndex<RegisteredCondition> ensureIndex() {
        if (conditionIndex == null) {
            synchronized (conditions) {
                if (conditionIndex == null) {
                    List<RegisteredCondition> copy = new ArrayList<>(conditions);
                    copy.sort(Comparator.comparingInt((RegisteredCondition rc) -> PatternRegistry.specificity(rc.pattern())).reversed());
                    conditionIndex = new PatternIndex<>(copy, RegisteredCondition::pattern);
                }
            }
        }
        return conditionIndex;
    }

    /**
     * Eagerly builds the internal pattern index so that the first match call
     * does not pay the sorting and indexing cost.
     */
    public void warmup() {
        ensureIndex();
    }
}
