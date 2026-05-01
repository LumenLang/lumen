package dev.lumenlang.lumen.plugin.util;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.LumenProvider;
import dev.lumenlang.lumen.api.codegen.TypeEnv.VarHandle;
import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.handler.StatementHandler;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.AddonTypeBinding;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

/**
 * Fluent builder for registering entity-specific statement, condition, and expression
 * patterns with minimal boilerplate.
 *
 * <p>All helpers share a common entity type that is set once via {@link #forType(String)}.
 * The builder then provides concise methods for registering common pattern shapes,
 * automatically handling {@code instanceof} guards, imports, and type validation.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * EntityHelper.forType("org.bukkit.entity.Wolf")
 *     .alias("_wf")
 *     .conditionPair(
 *         "%e:ENTITY% (is|is not) angry",
 *         0, "is",
 *         "isAngry()",
 *         "Checks if a wolf is or is not angry.",
 *         "if mob is angry:"
 *     )
 * }</pre>
 *
 * @see EntityValidation
 */
// TODO: Completely rewrite this
@SuppressWarnings("DataFlowIssue")
public final class EntityHelper {

    private final LumenAPI api;
    private final String fqcn;
    private final String simpleName;
    private String alias;

    private EntityHelper(@NotNull String fqcn) {
        this.api = Objects.requireNonNull(LumenProvider.api(), "LumenAPI is not available yet");
        this.fqcn = fqcn;
        this.simpleName = fqcn.substring(fqcn.lastIndexOf('.') + 1);
        this.alias = "_" + simpleName.substring(0, Math.min(2, simpleName.length())).toLowerCase(Locale.ROOT);
    }

    /**
     * Converts a pattern to use the possessive entity type binding.
     *
     * <p>For a pattern like {@code "set %e:ENTITY% health [to] %val:INT%"}, this returns
     * {@code "set %e:ENTITY_POSSESSIVE% health [to] %val:INT%"} so that the possessive
     * form is required (e.g. {@code entity's health}) for property access patterns.
     *
     * @param pattern the original pattern string
     * @return the pattern with ENTITY replaced by ENTITY_POSSESSIVE
     */
    private static @NotNull String toPossessive(@NotNull String pattern) {
        return pattern.replace(":ENTITY%", ":ENTITY_POSSESSIVE%");
    }

    /**
     * Converts optional {@code [get]} prefix to required {@code get} in a pattern.
     *
     * @param pattern the original pattern string
     * @return the pattern with {@code [get]} replaced by {@code get}
     */
    private static @NotNull String requireGet(@NotNull String pattern) {
        return pattern.replace("[get] ", "get ");
    }

    /**
     * Creates a new builder for entity patterns targeting the given Bukkit entity type.
     *
     * @param fqcn the fully qualified Bukkit entity class name
     * @return a new builder instance
     */
    public static @NotNull EntityHelper forType(@NotNull String fqcn) {
        return new EntityHelper(fqcn);
    }

    /**
     * Sets the instanceof alias variable name used in generated code.
     *
     * <p>If not set, defaults to the first two characters of the simple class name
     * in lowercase prefixed with {@code _} (e.g. {@code "_cr"} for Creeper).
     *
     * @param alias the alias (e.g. {@code "_wf"})
     * @return this builder
     */
    public @NotNull EntityHelper alias(@NotNull String alias) {
        this.alias = alias;
        return this;
    }

    /**
     * Registers a condition with both positive and negative forms using a single
     * required group pattern like {@code (is|is not)}.
     *
     * @param pattern        the pattern containing a required choice group for negation
     * @param choiceIndex    the zero-based index of the required group that determines negation
     * @param positiveChoice the text of the group choice representing the positive form (e.g. {@code "is"})
     * @param methodCall     the boolean getter method call (e.g. {@code "isPowered()"})
     * @param description    the description for the condition
     * @param example        the example usage
     * @return this builder
     */
    public @NotNull EntityHelper conditionPair(@NotNull String pattern,
                                               int choiceIndex,
                                               @NotNull String positiveChoice,
                                               @NotNull String methodCall,
                                               @NotNull String description,
                                               @NotNull String example) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern(pattern)
                .description(description)
                .example(example)
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    VarHandle h = ctx.requireVarHandle("e");
                    EntityValidation.requireSubtype(h, fqcn, pattern);
                    ctx.codegen().addImport(fqcn);
                    boolean negated = !positiveChoice.equals(ctx.choice(choiceIndex));
                    return "(" + h.java() + " instanceof " + simpleName + " " + alias
                            + " && " + (negated ? "!" : "") + alias + "." + methodCall + ")";
                }));

        return this;
    }

    /**
     * Registers a single condition with a custom handler.
     *
     * @param pattern     the condition pattern
     * @param description the description
     * @param example     the example
     * @param handler     the condition handler
     * @return this builder
     */
    public @NotNull EntityHelper condition(@NotNull String pattern,
                                           @NotNull String description,
                                           @NotNull String example,
                                           @NotNull ConditionHandler handler) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern(pattern)
                .description(description)
                .example(example)
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(handler));
        return this;
    }

    /**
     * Registers a statement that sets a boolean property on the typed entity.
     *
     * @param pattern     the statement pattern (must include {@code %val:BOOLEAN%})
     * @param setterName  the setter method name without parentheses
     * @param description the description
     * @param example     the example
     * @return this builder
     */
    public @NotNull EntityHelper boolSetter(@NotNull String pattern,
                                            @NotNull String setterName,
                                            @NotNull String description,
                                            @NotNull String example) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern(toPossessive(pattern))
                .description(description)
                .example(example)
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    EntityValidation.requireSubtype((VarHandle) ctx.value("e"), fqcn, setterName);
                    ctx.codegen().addImport(fqcn);
                    ctx.out().line("if (" + ctx.java("e") + " instanceof " + simpleName + " " + alias
                            + ") { " + alias + "." + setterName + "(" + ctx.java("val") + "); }");
                }));
        return this;
    }

    /**
     * Registers a statement that sets an integer property on the typed entity.
     *
     * @param pattern     the statement pattern (must include {@code %val:INT%})
     * @param setterName  the setter method name without parentheses
     * @param description the description
     * @param example     the example
     * @return this builder
     */
    public @NotNull EntityHelper intSetter(@NotNull String pattern,
                                           @NotNull String setterName,
                                           @NotNull String description,
                                           @NotNull String example) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern(toPossessive(pattern))
                .description(description)
                .example(example)
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    EntityValidation.requireSubtype((VarHandle) ctx.value("e"), fqcn, setterName);
                    ctx.codegen().addImport(fqcn);
                    ctx.out().line("if (" + ctx.java("e") + " instanceof " + simpleName + " " + alias
                            + ") { " + alias + "." + setterName + "(" + ctx.java("val") + "); }");
                }));
        return this;
    }

    /**
     * Registers a statement that calls a void no-arg method on the typed entity.
     *
     * @param pattern     the statement pattern
     * @param methodCall  the method call including parentheses
     * @param description the description
     * @param example     the example
     * @return this builder
     */
    public @NotNull EntityHelper action(@NotNull String pattern,
                                        @NotNull String methodCall,
                                        @NotNull String description,
                                        @NotNull String example) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern(pattern)
                .description(description)
                .example(example)
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    EntityValidation.requireSubtype((VarHandle) ctx.value("e"), fqcn, pattern);
                    ctx.codegen().addImport(fqcn);
                    ctx.out().line("if (" + ctx.java("e") + " instanceof " + simpleName + " " + alias
                            + ") { " + alias + "." + methodCall + "; }");
                }));
        return this;
    }

    /**
     * Registers a statement that sets an enum property using a typed enum binding.
     *
     * <p>This method expects the pattern to use a proper enum type
     * binding (e.g. {@code %color:DYE_COLOR%}) rather than a raw EXPR. The binding's
     * {@link AddonTypeBinding#toJava toJava} method handles
     * enum constant resolution and imports automatically.
     *
     * @param pattern      the statement pattern (should use an enum type binding)
     * @param setterMethod the setter method name (e.g. {@code "setColor"})
     * @param valBinding   the binding name for the enum value
     * @param description  the description
     * @param example      the example
     * @return this builder
     */
    public @NotNull EntityHelper typedEnumSetter(@NotNull String pattern,
                                                 @NotNull String setterMethod,
                                                 @NotNull String valBinding,
                                                 @NotNull String description,
                                                 @NotNull String example) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern(toPossessive(pattern))
                .description(description)
                .example(example)
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    EntityValidation.requireSubtype((VarHandle) ctx.value("e"), fqcn, pattern);
                    ctx.codegen().addImport(fqcn);
                    ctx.out().line("if (" + ctx.java("e") + " instanceof " + simpleName + " " + alias
                            + ") { " + alias + "." + setterMethod + "("
                            + ctx.java(valBinding) + "); }");
                }));
        return this;
    }

    /**
     * Registers a statement with a fully custom handler.
     *
     * @param pattern     the statement pattern
     * @param description the description
     * @param example     the example
     * @param handler     the statement handler
     * @return this builder
     */
    public @NotNull EntityHelper statement(@NotNull String pattern,
                                           @NotNull String description,
                                           @NotNull String example,
                                           @NotNull StatementHandler handler) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern(pattern)
                .description(description)
                .example(example)
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(handler));
        return this;
    }

    /**
     * Registers an expression that gets an integer property, returning 0 as a fallback.
     *
     * @param pattern     the expression pattern
     * @param getterCall  the getter method call
     * @param description the description
     * @param example     the example
     * @return this builder
     */
    public @NotNull EntityHelper intGetter(@NotNull String pattern,
                                           @NotNull String getterCall,
                                           @NotNull String description,
                                           @NotNull String example) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern(requireGet(toPossessive(pattern)))
                .description(description)
                .example(example)
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    String java = ctx.java("e");
                    EntityValidation.requireSubtype((VarHandle) ctx.value("e"), fqcn, pattern);
                    ctx.codegen().addImport(fqcn);
                    return new ExpressionResult(
                            "(" + java + " instanceof " + simpleName + " " + alias
                                    + " ? " + alias + "." + getterCall + " : 0)",
                            PrimitiveType.INT);
                }));
        return this;
    }

    /**
     * Registers an expression that gets a string property, returning null as a fallback.
     *
     * @param pattern     the expression pattern
     * @param getterCall  the getter method call
     * @param description the description
     * @param example     the example
     * @return this builder
     */
    public @NotNull EntityHelper stringGetter(@NotNull String pattern,
                                              @NotNull String getterCall,
                                              @NotNull String description,
                                              @NotNull String example) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern(requireGet(toPossessive(pattern)))
                .description(description)
                .example(example)
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    String java = ctx.java("e");
                    EntityValidation.requireSubtype((VarHandle) ctx.value("e"), fqcn, pattern);
                    ctx.codegen().addImport(fqcn);
                    return new ExpressionResult(
                            "(" + java + " instanceof " + simpleName + " " + alias
                                    + " ? " + alias + "." + getterCall + " : null)",
                            PrimitiveType.STRING);
                }));
        return this;
    }

    /**
     * Registers an expression that gets a boolean property.
     *
     * @param pattern     the expression pattern
     * @param getterCall  the boolean getter method call
     * @param description the description
     * @param example     the example
     * @return this builder
     */
    public @NotNull EntityHelper boolGetter(@NotNull String pattern,
                                            @NotNull String getterCall,
                                            @NotNull String description,
                                            @NotNull String example) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern(requireGet(toPossessive(pattern)))
                .description(description)
                .example(example)
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    String java = ctx.java("e");
                    EntityValidation.requireSubtype((VarHandle) ctx.value("e"), fqcn, pattern);
                    ctx.codegen().addImport(fqcn);
                    return new ExpressionResult(
                            "(" + java + " instanceof " + simpleName + " " + alias
                                    + " && " + alias + "." + getterCall + ")",
                            PrimitiveType.BOOLEAN);
                }));
        return this;
    }

    /**
     * Registers an expression with a fully custom handler.
     *
     * @param pattern     the expression pattern
     * @param description the description
     * @param example     the example
     * @param handler     the expression handler
     * @return this builder
     */
    public @NotNull EntityHelper expression(@NotNull String pattern,
                                            @NotNull String description,
                                            @NotNull String example,
                                            @NotNull ExpressionHandler handler) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern(pattern)
                .description(description)
                .example(example)
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(handler));
        return this;
    }
}
