package net.vansencool.lumen.plugin.util;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.LumenProvider;
import net.vansencool.lumen.api.handler.ConditionHandler;
import net.vansencool.lumen.api.handler.ExpressionHandler;
import net.vansencool.lumen.api.handler.StatementHandler;
import net.vansencool.lumen.api.pattern.Categories;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Fluent builder for registering inventory-specific statement, condition, and expression
 * patterns with minimal boilerplate.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * InventoryHelper.create()
 *     .statement(
 *         "set slot %slot:INT% of %inv:EXPR% to %item:ITEMSTACK%",
 *         "Sets an item in a specific slot of an inventory.",
 *         "set slot 0 of inv to sword",
 *         (line, ctx, out) -> { ... }
 *     )
 *     .expression(
 *         "[get] item in slot %slot:INT% of %inv:EXPR%",
 *         "Returns the item in the given slot.",
 *         "var item = get item in slot 0 of inv",
 *         ctx -> { ... }
 *     );
 * }</pre>
 *
 * @see EntityHelper
 */
public final class InventoryHelper {

    private final LumenAPI api;

    private InventoryHelper() {
        this.api = Objects.requireNonNull(LumenProvider.api(), "LumenAPI is not available yet");
    }

    /**
     * Creates a new inventory helper builder.
     *
     * @return a new builder instance
     */
    public static @NotNull InventoryHelper create() {
        return new InventoryHelper();
    }

    /**
     * Registers a statement pattern.
     *
     * @param pattern     the pattern string
     * @param description human-readable description
     * @param example     usage example
     * @param handler     the statement handler
     * @return this builder
     */
    public @NotNull InventoryHelper statement(@NotNull String pattern,
                                              @NotNull String description,
                                              @NotNull String example,
                                              @NotNull StatementHandler handler) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern(pattern)
                .description(description)
                .example(example)
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler(handler));
        return this;
    }

    /**
     * Registers a statement pattern with multiple pattern aliases.
     *
     * @param patterns    the pattern strings
     * @param description human-readable description
     * @param example     usage example
     * @param handler     the statement handler
     * @return this builder
     */
    public @NotNull InventoryHelper statement(@NotNull String[] patterns,
                                              @NotNull String description,
                                              @NotNull String example,
                                              @NotNull StatementHandler handler) {
        api.patterns().statement(b -> {
            b.by("Lumen")
                    .description(description)
                    .example(example)
                    .since("1.0.0")
                    .category(Categories.INVENTORY)
                    .handler(handler);
            for (String p : patterns) {
                b.pattern(p);
            }
        });
        return this;
    }

    /**
     * Registers a condition pattern.
     *
     * @param pattern     the pattern string
     * @param description human-readable description
     * @param example     usage example
     * @param handler     the condition handler
     * @return this builder
     */
    public @NotNull InventoryHelper condition(@NotNull String pattern,
                                              @NotNull String description,
                                              @NotNull String example,
                                              @NotNull ConditionHandler handler) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern(pattern)
                .description(description)
                .example(example)
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler(handler));
        return this;
    }

    /**
     * Registers a positive/negative condition pair.
     *
     * @param positivePattern     the pattern for the positive check
     * @param negativePattern     the pattern for the negative check
     * @param positiveDescription description of the positive condition
     * @param negativeDescription description of the negative condition
     * @param positiveExample     example for the positive condition
     * @param negativeExample     example for the negative condition
     * @param positiveHandler     handler for the positive condition
     * @param negativeHandler     handler for the negative condition
     * @return this builder
     */
    public @NotNull InventoryHelper conditionPair(@NotNull String positivePattern,
                                                  @NotNull String negativePattern,
                                                  @NotNull String positiveDescription,
                                                  @NotNull String negativeDescription,
                                                  @NotNull String positiveExample,
                                                  @NotNull String negativeExample,
                                                  @NotNull ConditionHandler positiveHandler,
                                                  @NotNull ConditionHandler negativeHandler) {
        condition(positivePattern, positiveDescription, positiveExample, positiveHandler);
        condition(negativePattern, negativeDescription, negativeExample, negativeHandler);
        return this;
    }

    /**
     * Registers an expression pattern.
     *
     * @param pattern     the pattern string
     * @param description human-readable description
     * @param example     usage example
     * @param handler     the expression handler
     * @return this builder
     */
    public @NotNull InventoryHelper expression(@NotNull String pattern,
                                               @NotNull String description,
                                               @NotNull String example,
                                               @NotNull ExpressionHandler handler) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern(pattern)
                .description(description)
                .example(example)
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler(handler));
        return this;
    }

    /**
     * Returns the underlying API instance for advanced registrations that
     * do not fit the helper's convenience methods.
     *
     * @return the LumenAPI
     */
    public @NotNull LumenAPI api() {
        return api;
    }
}
