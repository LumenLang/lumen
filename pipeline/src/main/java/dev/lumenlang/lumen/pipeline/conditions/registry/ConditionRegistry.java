package dev.lumenlang.lumen.pipeline.conditions.registry;

import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.pattern.PatternMeta;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.compile.PatternCompiler;
import dev.lumenlang.lumen.pipeline.language.match.InlineExprValidator;
import dev.lumenlang.lumen.pipeline.language.match.Match;
import dev.lumenlang.lumen.pipeline.language.match.PatternMatcher;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternPart;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
    private volatile boolean sorted;
    private @Nullable InlineExprValidator validator;

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
    public RegisteredConditionMatch match(List<Token> tokens, TypeEnv env) {
        ensureSorted();
        for (RegisteredCondition rc : conditions) {
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
    public @Nullable RegisteredConditionMatch matchSlow(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
        if (validator == null) return null;
        ensureSorted();
        for (RegisteredCondition rc : conditions) {
            Match m = PatternMatcher.match(tokens, rc.pattern(), types, env, validator);
            if (m != null) return new RegisteredConditionMatch(rc, m);
        }
        return null;
    }

    private void ensureSorted() {
        if (!sorted) {
            synchronized (conditions) {
                if (!sorted) {
                    conditions.sort(Comparator.comparingInt(
                            (RegisteredCondition rc) -> PatternRegistry.specificity(rc.pattern())).reversed());
                    sorted = true;
                }
            }
        }
    }
}
