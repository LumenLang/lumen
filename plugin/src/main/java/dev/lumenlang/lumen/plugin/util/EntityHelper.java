package dev.lumenlang.lumen.plugin.util;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.LumenProvider;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess.VarHandle;
import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.handler.StatementHandler;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.AddonTypeBinding;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
 *         "%e:ENTITY% is angry", "%e:ENTITY% is not angry",
 *         "isAngry()",
 *         "Checks if a wolf is angry.", "Checks if a wolf is not angry.",
 *         "if mob is angry:", "if mob is not angry:"
 *     )
 *     .boolSetter(
 *         "set %e:ENTITY% angry [to] %val:EXPR%",
 *         "setAngry",
 *         "Sets whether a wolf is angry.",
 *         "set mob angry to true"
 *     )
 *     .stringGetter(
 *         "[get] %e:ENTITY% collar color",
 *         "getCollarColor().name()",
 *         "Returns the wolf's collar color name.",
 *         "var c = mob collar color"
 *     );
 * }</pre>
 *
 * @see EntityValidation
 */
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
    // TODO: Replace all [get] occurrences in the plugin instead of relying on this method
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
     * Registers a positive/negative condition pair for a boolean entity check.
     *
     * @param positivePattern     the pattern for the positive condition
     * @param negativePattern     the pattern for the negative condition
     * @param methodCall          the boolean getter method call (e.g. {@code "isPowered()"})
     * @param positiveDescription the description for the positive condition
     * @param negativeDescription the description for the negative condition
     * @param positiveExample     the example for the positive condition
     * @param negativeExample     the example for the negative condition
     * @return this builder
     */
    public @NotNull EntityHelper conditionPair(@NotNull String positivePattern,
                                               @NotNull String negativePattern,
                                               @NotNull String methodCall,
                                               @NotNull String positiveDescription,
                                               @NotNull String negativeDescription,
                                               @NotNull String positiveExample,
                                               @NotNull String negativeExample) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern(positivePattern)
                .description(positiveDescription)
                .example(positiveExample)
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> {
                    VarHandle h = match.ref("e");
                    EntityValidation.requireSubtype(h, fqcn, positivePattern);
                    ctx.addImport(fqcn);
                    return "(" + h.java() + " instanceof " + simpleName + " " + alias
                            + " && " + alias + "." + methodCall + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern(negativePattern)
                .description(negativeDescription)
                .example(negativeExample)
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> {
                    VarHandle h = match.ref("e");
                    EntityValidation.requireSubtype(h, fqcn, negativePattern);
                    ctx.addImport(fqcn);
                    return "(" + h.java() + " instanceof " + simpleName + " " + alias
                            + " && !" + alias + "." + methodCall + ")";
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
                .handler((line, ctx, out) -> {
                    EntityValidation.requireSubtype((VarHandle) ctx.value("e"), fqcn, setterName);
                    ctx.codegen().addImport(fqcn);
                    out.line("if (" + ctx.java("e") + " instanceof " + simpleName + " " + alias
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
                .handler((line, ctx, out) -> {
                    EntityValidation.requireSubtype((VarHandle) ctx.value("e"), fqcn, setterName);
                    ctx.codegen().addImport(fqcn);
                    out.line("if (" + ctx.java("e") + " instanceof " + simpleName + " " + alias
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
                .handler((line, ctx, out) -> {
                    EntityValidation.requireSubtype((VarHandle) ctx.value("e"), fqcn, pattern);
                    ctx.codegen().addImport(fqcn);
                    out.line("if (" + ctx.java("e") + " instanceof " + simpleName + " " + alias
                            + ") { " + alias + "." + methodCall + "; }");
                }));
        return this;
    }

    /**
     * Registers a statement that sets an enum property from a string expression.
     *
     * <p>The value binding has its quotes stripped and is converted to uppercase to produce
     * a valid enum constant name. Use {@code {val}} as a placeholder for the enum constant.
     *
     * @param pattern     the statement pattern
     * @param setterExpr  the setter expression with {@code {val}} placeholder
     * @param valBinding  the binding name for the value
     * @param extraImport an additional import for the enum class, or null
     * @param description the description
     * @param example     the example
     * @return this builder
     */
    public @NotNull EntityHelper enumSetter(@NotNull String pattern,
                                            @NotNull String setterExpr,
                                            @NotNull String valBinding,
                                            @Nullable String extraImport,
                                            @NotNull String description,
                                            @NotNull String example) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern(toPossessive(pattern))
                .description(description)
                .example(example)
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> {
                    EntityValidation.requireSubtype((VarHandle) ctx.value("e"), fqcn, pattern);
                    ctx.codegen().addImport(fqcn);
                    if (extraImport != null) ctx.codegen().addImport(extraImport);
                    String val = ctx.java(valBinding).replace("\"", "").toUpperCase(Locale.ROOT);
                    String resolved = setterExpr.replace("{val}", val);
                    out.line("if (" + ctx.java("e") + " instanceof " + simpleName + " " + alias
                            + ") { " + alias + "." + resolved + "; }");
                }));
        return this;
    }

    /**
     * Registers a statement that sets an enum property using a typed enum binding.
     *
     * <p>Unlike {@link #enumSetter}, this method expects the pattern to use a proper enum type
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
                .handler((line, ctx, out) -> {
                    EntityValidation.requireSubtype((VarHandle) ctx.value("e"), fqcn, pattern);
                    ctx.codegen().addImport(fqcn);
                    out.line("if (" + ctx.java("e") + " instanceof " + simpleName + " " + alias
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
                            null);
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
        return stringGetter(pattern, getterCall, null, description, example);
    }

    /**
     * Registers an expression that gets a string property with a specific result ref type.
     *
     * @param pattern     the expression pattern
     * @param getterCall  the getter method call
     * @param refTypeId   the ref type id for the result, or null
     * @param description the description
     * @param example     the example
     * @return this builder
     */
    public @NotNull EntityHelper stringGetter(@NotNull String pattern,
                                              @NotNull String getterCall,
                                              @Nullable String refTypeId,
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
                            refTypeId);
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
                            null);
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
