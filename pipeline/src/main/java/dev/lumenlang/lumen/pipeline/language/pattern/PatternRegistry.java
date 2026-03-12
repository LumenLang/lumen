package dev.lumenlang.lumen.pipeline.language.pattern;

import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.handler.LoopHandler;
import dev.lumenlang.lumen.api.handler.StatementHandler;
import dev.lumenlang.lumen.api.pattern.PatternMeta;
import dev.lumenlang.lumen.api.pattern.builder.BlockBuilder;
import dev.lumenlang.lumen.api.pattern.builder.ConditionBuilder;
import dev.lumenlang.lumen.api.pattern.builder.ExpressionBuilder;
import dev.lumenlang.lumen.api.pattern.builder.LoopBuilder;
import dev.lumenlang.lumen.api.pattern.builder.StatementBuilder;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.conditions.registry.ConditionRegistry;
import dev.lumenlang.lumen.pipeline.language.compile.PatternCompiler;
import dev.lumenlang.lumen.pipeline.language.match.InlineExprValidator;
import dev.lumenlang.lumen.pipeline.language.match.Match;
import dev.lumenlang.lumen.pipeline.language.match.PatternMatcher;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.loop.LoopRegistry;
import dev.lumenlang.lumen.pipeline.loop.RegisteredLoopMatch;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Central registry for all Lumen patterns including statements, blocks, and
 * conditions.
 *
 * <p>
 * The PatternRegistry is the heart of Lumen's extensibility system. It allows
 * patterns to
 * be registered with handlers that define how matching script code should be
 * compiled into Java.
 *
 * <h2>Pattern Types</h2>
 * <p>
 * There are three types of patterns:
 * <ul>
 * <li><b>Statements:</b> Single-line actions like
 * {@code "give player diamond 1"}</li>
 * <li><b>Blocks:</b> Multi-line structures like {@code "on join:"} with
 * indented children</li>
 * <li><b>Conditions:</b> Boolean expressions used in if statements</li>
 * </ul>
 *
 * <h2>Registration Example</h2>
 *
 * <pre>{@code
 * PatternRegistry reg = new PatternRegistry(typeRegistry);
 *
 * // Register a statement
 * reg.statements("give %who:PLAYER% %item:MATERIAL% %amt:INT%", (node, ctx, out) -> {
 *     out.line(ctx.java("who") + ".getInventory().addItem(new ItemStack(" +
 *             ctx.java("item") + ", " + ctx.java("amt") + "));");
 * });
 *
 * // Register a block
 * reg.block("on %events:EXPR%", new BlockHandler() {
 *     public void begin(BindingContext ctx, JavaBuilder out) {
 *         out.line("public void handle_" + ctx.java("events") + "() {");
 *     }
 *
 *     public void end(BindingContext ctx, JavaBuilder out) {
 *         out.line("}");
 *     }
 * });
 *
 * // Register a condition
 * reg.condition("%p:PLAYER% health > %n:INT%", (m, env, ctx) -> {
 *     return m.values().get("p").value() + ".getHealth() > " +
 *             m.values().get("n").value();
 * });
 * }</pre>
 *
 * <h2>Pattern Matching Order</h2>
 * <p>
 * When matching a statement or block, the registry tries patterns in
 * registration order.
 * The first pattern that successfully matches is used. This means:
 * <ul>
 * <li>More specific patterns should be registered before generic ones</li>
 * <li>Pattern order matters for ambiguous cases</li>
 * </ul>
 *
 * <h2>Singleton Instance</h2>
 * <p>
 * The PatternRegistry uses a singleton pattern accessible via
 * {@link #instance()}.
 * This must be initialized during plugin startup with
 * {@link #instance(PatternRegistry)}.
 *
 * @see #statement(String, StatementHandler)
 * @see #block(String, BlockHandler)
 * @see #condition(String, ConditionHandler)
 */
@SuppressWarnings("unused")
public final class PatternRegistry {
    private static PatternRegistry INSTANCE;
    private final List<RegisteredBlock> blocks = new ArrayList<>();
    private final List<RegisteredPattern> statements = new ArrayList<>();
    private final List<RegisteredExpression> expressions = new ArrayList<>();
    private final ConditionRegistry conditionRegistry;
    private final LoopRegistry loopRegistry;
    private final TypeRegistry types;
    private volatile List<RegisteredPattern> sortedStatements;
    private volatile List<RegisteredExpression> sortedExpressions;

    public PatternRegistry(@NotNull TypeRegistry types) {
        this.types = types;
        conditionRegistry = new ConditionRegistry(types);
        conditionRegistry.setInlineExprValidator((toks, e) -> matchExpressionFast(toks, e) != null);
        loopRegistry = new LoopRegistry(types);
        loopRegistry.setInlineExprValidator((toks, e) -> matchExpressionFast(toks, e) != null);
    }

    /**
     * Sets the global singleton instance of PatternRegistry.
     *
     * <p>
     * This must be called during plugin initialization before any pattern matching
     * occurs.
     *
     * @param instance the PatternRegistry instance to use globally
     */
    public static void instance(@NotNull PatternRegistry instance) {
        PatternRegistry.INSTANCE = instance;
    }

    /**
     * Returns the global singleton instance of PatternRegistry.
     *
     * @return the global PatternRegistry instance
     * @throws RuntimeException if called before initialization
     */
    public static PatternRegistry instance() {
        if (INSTANCE == null) {
            throw new RuntimeException("Called PatternRegistry#instance before plugin enables or in type bindings.");
        }
        return PatternRegistry.INSTANCE;
    }

    /**
     * Computes a specificity score for a pattern. Patterns with more literal parts are
     * considered more specific and should be tried first during matching.
     *
     * @param pattern the pattern to score
     * @return the specificity score (higher means more specific)
     */
    public static int specificity(@NotNull Pattern pattern) {
        int score = 0;
        for (PatternPart part : pattern.parts()) {
            score += partSpecificity(part);
        }
        return score;
    }

    private static int partSpecificity(@NotNull PatternPart part) {
        if (part instanceof PatternPart.Literal) return 2;
        if (part instanceof PatternPart.FlexLiteral) return 2;
        if (part instanceof PatternPart.PlaceholderPart pp) {
            return pp.ph().typeId().equals("EXPR") ? 0 : 1;
        }
        if (part instanceof PatternPart.Group group) {
            if (!group.required()) return 0;
            int best = 0;
            for (List<PatternPart> alt : group.alternatives()) {
                int altScore = 0;
                for (PatternPart altPart : alt) {
                    altScore += partSpecificity(altPart);
                }
                best = Math.max(best, altScore);
            }
            return best;
        }
        return 0;
    }

    /**
     * Returns an unmodifiable view of all registered statement.
     *
     * @return the list of registered statements
     */
    public @NotNull List<RegisteredPattern> getStatements() {
        return List.copyOf(statements);
    }

    /**
     * Returns an unmodifiable view of all registered blocks.
     *
     * @return the list of registered blocks
     */
    public @NotNull List<RegisteredBlock> getBlocks() {
        return List.copyOf(blocks);
    }

    /**
     * Returns an unmodifiable view of all registered expressions.
     *
     * @return the list of registered expressions
     */
    public @NotNull List<RegisteredExpression> getExpressions() {
        return List.copyOf(expressions);
    }

    /**
     * Returns the condition registry used by this pattern registry.
     *
     * @return the condition registry
     */
    public @NotNull ConditionRegistry getConditionRegistry() {
        return conditionRegistry;
    }

    /**
     * Returns the loop registry used by this pattern registry.
     *
     * @return the loop registry
     */
    public @NotNull LoopRegistry getLoopRegistry() {
        return loopRegistry;
    }

    /**
     * Returns the type registry used by this pattern registry.
     *
     * @return the type registry
     */
    public @NotNull TypeRegistry getTypeRegistry() {
        return types;
    }

    /**
     * Registers a block pattern with its handler.
     *
     * <p>
     * Block patterns represent multi-line structures that contain child statement.
     * Examples include event handlers, if statements, and command definitions.
     *
     * @param pattern the pattern string (e.g., "on %events:EXPR%")
     * @param h       the handler that generates Java code for this block
     */
    public void block(String pattern, BlockHandler h) {
        Pattern compiled = PatternCompiler.compile(pattern);
        validateTypes(compiled);
        blocks.add(new RegisteredBlock(compiled, h));
    }

    /**
     * Registers multiple block pattern strings that all map to the same handler.
     *
     * @param patterns the list of pattern strings
     * @param h        the handler that generates Java code for these blocks
     */
    public void block(@NotNull List<String> patterns, @NotNull BlockHandler h) {
        for (String pattern : patterns) {
            Pattern compiled = PatternCompiler.compile(pattern);
            validateTypes(compiled);
            blocks.add(new RegisteredBlock(compiled, h));
        }
    }

    /**
     * Registers a statement pattern with its handler.
     *
     * <p>
     * Statement patterns represent single-line actions like giving items, sending
     * messages,
     * or modifying player properties.
     *
     * <p>
     * The handler is called once when the statement is encountered during code
     * generation.
     *
     * @param pattern the pattern string (e.g., "give %who:PLAYER% %item:MATERIAL%
     *                %amt:INT%")
     * @param h       the handler that generates Java code for this statement
     */
    public void statement(String pattern, StatementHandler h) {
        Pattern compiled = PatternCompiler.compile(pattern);
        validateTypes(compiled);
        statements.add(new RegisteredPattern(compiled, h));
    }

    /**
     * Registers multiple pattern strings that all map to the same statement
     * handler.
     *
     * @param patterns the list of pattern strings
     * @param h        the handler that generates Java code for these statements
     */
    public void statement(@NotNull List<String> patterns, @NotNull StatementHandler h) {
        for (String pattern : patterns) {
            Pattern compiled = PatternCompiler.compile(pattern);
            validateTypes(compiled);
            statements.add(new RegisteredPattern(compiled, h));
        }
    }

    /**
     * Registers a condition pattern with its handler.
     *
     * <p>
     * Condition patterns represent boolean expressions used in if/else-if
     * statement.
     * The handler must return a Java boolean expression as a string.
     *
     * @param pattern the pattern string (e.g., "%p:PLAYER% health > %n:INT%")
     * @param handler the handler that generates a Java boolean expression
     */
    public void condition(@NotNull String pattern, @NotNull ConditionHandler handler) {
        conditionRegistry.register(pattern, handler);
    }

    /**
     * Registers multiple condition pattern strings that all map to the same
     * handler.
     *
     * @param patterns the list of pattern strings
     * @param handler  the handler that generates a Java boolean expression
     */
    public void condition(@NotNull List<String> patterns, @NotNull ConditionHandler handler) {
        for (String pattern : patterns) {
            conditionRegistry.register(pattern, handler);
        }
    }

    /**
     * Registers an expression pattern with its handler.
     *
     * <p>
     * Expression patterns are used in {@code var x = <pattern>} contexts where the
     * pattern handler returns a Java expression that evaluates to the variable's
     * value.
     *
     * @param pattern the pattern string (e.g., "get player %name:STRING%")
     * @param handler the handler that returns a Java expression string
     */
    public void expression(@NotNull String pattern, @NotNull ExpressionHandler handler) {
        Pattern compiled = PatternCompiler.compile(pattern);
        validateTypes(compiled);
        expressions.add(new RegisteredExpression(compiled, handler));
    }

    /**
     * Registers multiple expression pattern strings that all map to the same
     * handler.
     *
     * @param patterns the list of pattern strings
     * @param handler  the handler that returns a Java expression result
     */
    public void expression(@NotNull List<String> patterns, @NotNull ExpressionHandler handler) {
        for (String pattern : patterns) {
            Pattern compiled = PatternCompiler.compile(pattern);
            validateTypes(compiled);
            expressions.add(new RegisteredExpression(compiled, handler));
        }
    }

    /**
     * Registers a statement pattern using a builder for documentation metadata.
     *
     * @param builderConsumer consumer that configures the builder
     */
    public void statement(@NotNull Consumer<StatementBuilder> builderConsumer) {
        StatementBuilder builder = new StatementBuilder();
        builderConsumer.accept(builder);
        builder.validate();
        PatternMeta meta = builder.buildMeta();
        for (String p : builder.getPatterns()) {
            Pattern compiled = PatternCompiler.compile(p);
            validateTypes(compiled);
            statements.add(new RegisteredPattern(compiled, builder.getHandler(), meta));
        }
    }

    /**
     * Registers a block pattern using a builder for documentation metadata.
     *
     * @param builderConsumer consumer that configures the builder
     */
    public void block(@NotNull Consumer<BlockBuilder> builderConsumer) {
        BlockBuilder builder = new BlockBuilder();
        builderConsumer.accept(builder);
        builder.validate();
        PatternMeta meta = builder.buildMeta();
        for (String p : builder.getPatterns()) {
            Pattern compiled = PatternCompiler.compile(p);
            validateTypes(compiled);
            blocks.add(new RegisteredBlock(compiled, builder.getHandler(), meta));
        }
    }

    /**
     * Registers a condition pattern using a builder for documentation metadata.
     *
     * @param builderConsumer consumer that configures the builder
     */
    public void condition(@NotNull Consumer<ConditionBuilder> builderConsumer) {
        ConditionBuilder builder = new ConditionBuilder();
        builderConsumer.accept(builder);
        builder.validate();
        PatternMeta meta = builder.buildMeta();
        for (String p : builder.getPatterns()) {
            conditionRegistry.register(p, builder.getHandler(), meta);
        }
    }

    /**
     * Registers an expression pattern using a builder for documentation metadata.
     *
     * @param builderConsumer consumer that configures the builder
     */
    public void expression(@NotNull Consumer<ExpressionBuilder> builderConsumer) {
        ExpressionBuilder builder = new ExpressionBuilder();
        builderConsumer.accept(builder);
        builder.validate();
        PatternMeta meta = builder.buildMeta();
        for (String p : builder.getPatterns()) {
            Pattern compiled = PatternCompiler.compile(p);
            validateTypes(compiled);
            expressions.add(new RegisteredExpression(compiled, builder.getHandler(), meta));
        }
    }

    public void loop(@NotNull String pattern, @NotNull LoopHandler handler) {
        loopRegistry.register(pattern, handler);
    }

    public void loop(@NotNull List<String> patterns, @NotNull LoopHandler handler) {
        for (String pattern : patterns) {
            loopRegistry.register(pattern, handler);
        }
    }

    public void loop(@NotNull Consumer<LoopBuilder> builderConsumer) {
        LoopBuilder builder = new LoopBuilder();
        builderConsumer.accept(builder);
        builder.validate();
        PatternMeta meta = builder.buildMeta();
        for (String p : builder.getPatterns()) {
            loopRegistry.register(p, builder.getHandler(), meta);
        }
    }

    /**
     * Attempts to match the given tokens against registered loop source patterns.
     *
     * @param tokens the loop source tokens
     * @param env    the current type environment
     * @return a match on success, or null
     */
    public @Nullable RegisteredLoopMatch matchLoop(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
        return loopRegistry.match(tokens, env);
    }

    /**
     * Slow path for loop matching that supports inline expressions.
     * Should only be called when the fast path ({@link #matchLoop}) returned null.
     *
     * @param tokens the loop source tokens
     * @param env    the type environment for variable lookups
     * @return a RegisteredLoopMatch if successful, null otherwise
     */
    public @Nullable RegisteredLoopMatch matchLoopSlow(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
        return loopRegistry.matchSlow(tokens, env);
    }

    /**
     * Returns the condition registry for pattern matching in boolean expressions.
     *
     * @return the condition registry
     */
    public ConditionRegistry conditionRegistry() {
        return conditionRegistry;
    }

    /**
     * Returns the type registry for looking up type bindings.
     *
     * @return the type registry
     */
    public TypeRegistry typeRegistry() {
        return types;
    }

    /**
     * Validates that all type bindings referenced in the given pattern exist in the type registry.
     *
     * @param pattern the compiled pattern to validate
     * @throws IllegalArgumentException if a referenced type binding does not exist
     */
    private void validateTypes(@NotNull Pattern pattern) {
        List<String> missing = new ArrayList<>();
        collectMissingTypes(pattern.parts(), missing);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Pattern '" + pattern.raw()
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
     * Attempts to match tokens against all registered block patterns.
     *
     * <p>
     * Block patterns are tried in registration order. The first successful match is
     * returned.
     *
     * @param tokens the tokens to match
     * @param env    the type environment for variable lookups
     * @return a RegisteredBlockMatch if successful, null otherwise
     */
    public RegisteredBlockMatch matchBlock(List<Token> tokens, TypeEnv env) {
        for (RegisteredBlock rb : blocks) {
            Match m = PatternMatcher.match(tokens, rb.pattern(), types, env);
            if (m != null)
                return new RegisteredBlockMatch(rb, m);
        }
        return null;
    }

    /**
     * Attempts to match tokens against all registered statement patterns.
     *
     * <p>
     * Statement patterns are tried in registration order. The first successful
     * match is returned.
     *
     * <p>
     * <b>Debug Output:</b> This method logs each pattern that is attempted and
     * whether it matched.
     *
     * @param tokens the tokens to match
     * @param env    the type environment for variable lookups
     * @return a RegisteredPatternMatch if successful, null otherwise
     */
    public RegisteredPatternMatch matchStatement(List<Token> tokens, TypeEnv env) {
        for (RegisteredPattern rp : ensureStatementsSorted()) {
            Match m = PatternMatcher.match(tokens, rp.pattern(), types, env);
            if (m != null) return new RegisteredPatternMatch(rp, m);
        }
        return null;
    }

    /**
     * Slow path for statement matching that supports inline expressions.
     * Should only be called when the fast path ({@link #matchStatement}) returned null.
     *
     * @param tokens the tokens to match
     * @param env    the type environment for variable lookups
     * @return a RegisteredPatternMatch if successful, null otherwise
     */
    public @Nullable RegisteredPatternMatch matchStatementSlow(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
        InlineExprValidator validator = (toks, e) -> matchExpressionFast(toks, e) != null;
        for (RegisteredPattern rp : ensureStatementsSorted()) {
            Match m = PatternMatcher.match(tokens, rp.pattern(), types, env, validator);
            if (m != null) return new RegisteredPatternMatch(rp, m);
        }
        return null;
    }

    private @NotNull List<RegisteredPattern> ensureStatementsSorted() {
        if (sortedStatements == null) {
            synchronized (statements) {
                if (sortedStatements == null) {
                    List<RegisteredPattern> copy = new ArrayList<>(statements);
                    copy.sort(Comparator.comparingInt(
                            (RegisteredPattern rp) -> specificity(rp.pattern())).reversed());
                    sortedStatements = Collections.unmodifiableList(copy);
                }
            }
        }
        return sortedStatements;
    }

    /**
     * Attempts to match tokens against all registered expression patterns.
     *
     * <p>
     * Expression patterns are tried in registration order. The first successful
     * match
     * is returned. Used for resolving {@code var x = <expression pattern>}
     * statement.
     *
     * @param tokens the tokens to match
     * @param env    the type environment for variable lookups
     * @return a RegisteredExpressionMatch if successful, null otherwise
     */
    public @Nullable RegisteredExpressionMatch matchExpression(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
        for (RegisteredExpression re : ensureExpressionsSorted()) {
            Match m = PatternMatcher.match(tokens, re.pattern(), types, env);
            if (m != null) return new RegisteredExpressionMatch(re, m);
        }
        return null;
    }

    /**
     * Slow path for expression matching that supports inline expressions.
     * Should only be called when the fast path ({@link #matchExpression}) returned null.
     *
     * @param tokens the tokens to match
     * @param env    the type environment for variable lookups
     * @return a RegisteredExpressionMatch if successful, null otherwise
     */
    public @Nullable RegisteredExpressionMatch matchExpressionSlow(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
        InlineExprValidator validator = (toks, e) -> matchExpressionFast(toks, e) != null;
        for (RegisteredExpression re : ensureExpressionsSorted()) {
            Match m = PatternMatcher.match(tokens, re.pattern(), types, env, validator);
            if (m != null) return new RegisteredExpressionMatch(re, m);
        }
        return null;
    }

    private @NotNull List<RegisteredExpression> ensureExpressionsSorted() {
        if (sortedExpressions == null) {
            synchronized (expressions) {
                if (sortedExpressions == null) {
                    List<RegisteredExpression> copy = new ArrayList<>(expressions);
                    copy.sort(Comparator.comparingInt(
                            (RegisteredExpression re) -> specificity(re.pattern())).reversed());
                    sortedExpressions = Collections.unmodifiableList(copy);
                }
            }
        }
        return sortedExpressions;
    }

    /**
     * Fast expression matching without InlineExpr support.
     * Used as the validator callback for InlineExpr boundary resolution.
     *
     * @param tokens the tokens to match
     * @param env    the type environment for variable lookups
     * @return a RegisteredExpressionMatch if successful, null otherwise
     */
    private @Nullable RegisteredExpressionMatch matchExpressionFast(
            @NotNull List<Token> tokens, @NotNull TypeEnv env) {
        for (RegisteredExpression re : ensureExpressionsSorted()) {
            Match m = PatternMatcher.match(tokens, re.pattern(), types, env);
            if (m != null)
                return new RegisteredExpressionMatch(re, m);
        }
        return null;
    }
}
