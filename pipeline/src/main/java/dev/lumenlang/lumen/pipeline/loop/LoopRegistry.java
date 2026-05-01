package dev.lumenlang.lumen.pipeline.loop;

import dev.lumenlang.lumen.api.handler.LoopHandler;
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
import java.util.List;

/**
 * Registry for all loop source patterns available in a Lumen script.
 *
 * <p>Loop source patterns define what a {@code loop ... in <source>:} block iterates over.
 * Patterns are tried in registration order; the first match wins.
 *
 * <p>This registry is owned by the
 * {@link PatternRegistry}
 * and should not be instantiated independently.
 *
 * @see RegisteredLoop
 */
public class LoopRegistry {

    private final List<RegisteredLoop> loops = new ArrayList<>();
    private final TypeRegistry types;
    private volatile PatternIndex<RegisteredLoop> loopIndex;
    private @Nullable InlineExprValidator validator;

    /**
     * Creates a new registry backed by the given type registry.
     *
     * @param types the type registry used during pattern matching
     */
    public LoopRegistry(@NotNull TypeRegistry types) {
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
     * Registers a loop source pattern with its handler and documentation metadata.
     *
     * @param pattern the raw pattern string (e.g. {@code "all players"})
     * @param handler the handler that generates the iterable expression
     * @param meta    documentation metadata for this loop source
     */
    public void register(@NotNull String pattern, @NotNull LoopHandler handler, @NotNull PatternMeta meta) {
        Pattern compiled = PatternCompiler.compile(pattern);
        validateTypes(compiled);
        loops.add(new RegisteredLoop(compiled, handler, meta));
        loopIndex = null;
    }

    /**
     * Registers a loop source pattern with its handler, without documentation metadata.
     *
     * @param pattern the raw pattern string
     * @param handler the handler that generates the iterable expression
     */
    public void register(@NotNull String pattern, @NotNull LoopHandler handler) {
        register(pattern, handler, PatternMeta.EMPTY);
    }

    private void validateTypes(@NotNull Pattern pattern) {
        List<String> missing = new ArrayList<>();
        collectMissingTypes(pattern.parts(), missing);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Loop pattern '" + pattern.raw()
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
     * Returns an unmodifiable view of all registered loop sources.
     *
     * @return the list of registered loop sources
     */
    public @NotNull List<RegisteredLoop> getLoops() {
        return List.copyOf(loops);
    }

    /**
     * Attempts to match the given tokens against all registered loop source patterns.
     *
     * <p>Patterns are tried in registration order. The first successful match is returned.
     *
     * @param tokens the loop source token list to match
     * @param env    the current type environment
     * @return a {@link RegisteredLoopMatch} on success, or {@code null} if no pattern matched
     */
    public @Nullable RegisteredLoopMatch match(@NotNull List<Token> tokens, @NotNull TypeEnvImpl env) {
        for (RegisteredLoop rl : ensureIndex().candidates(tokens)) {
            Match m = PatternMatcher.match(tokens, rl.pattern(), types, env);
            if (m != null) return new RegisteredLoopMatch(rl, m);
        }
        return null;
    }

    /**
     * Slow path for loop matching that supports inline expressions.
     * Should only be called when the fast path ({@link #match}) returned null.
     *
     * @param tokens the loop source token list to match
     * @param env    the current type environment
     * @return a {@link RegisteredLoopMatch} on success, or {@code null} if no pattern matched
     */
    public @Nullable RegisteredLoopMatch matchSlow(@NotNull List<Token> tokens, @NotNull TypeEnvImpl env) {
        if (validator == null) return null;
        for (RegisteredLoop rl : ensureIndex().candidates(tokens)) {
            Match m = PatternMatcher.match(tokens, rl.pattern(), types, env, validator);
            if (m != null) return new RegisteredLoopMatch(rl, m);
        }
        return null;
    }

    private @NotNull PatternIndex<RegisteredLoop> ensureIndex() {
        if (loopIndex == null) {
            synchronized (loops) {
                if (loopIndex == null) {
                    List<RegisteredLoop> copy = new ArrayList<>(loops);
                    copy.sort(Comparator.comparingInt((RegisteredLoop rl) -> PatternRegistry.specificity(rl.pattern())).reversed());
                    loopIndex = new PatternIndex<>(copy, RegisteredLoop::pattern);
                }
            }
        }
        return loopIndex;
    }

    /**
     * Eagerly builds the internal pattern index so that the first match call
     * does not pay the sorting and indexing cost.
     */
    public void warmup() {
        ensureIndex();
    }
}
