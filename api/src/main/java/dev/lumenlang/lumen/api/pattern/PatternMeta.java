package dev.lumenlang.lumen.api.pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Immutable documentation metadata attached to a registered pattern.
 *
 * <p>
 * Every registered statement, expression, condition, or block can carry a
 * {@code PatternMeta} that describes what the pattern does, how to use it,
 * and when it was introduced. This information is used for automatic
 * documentation generation.
 *
 * @param by          the addon name that registered this pattern (e.g. "Lumen")
 * @param description a human-readable description of what the pattern does
 * @param examples    one or more Lumen script examples showing usage
 * @param since       the version in which this pattern was introduced
 * @param category    the documentation category this pattern belongs to
 * @param deprecated  whether this pattern is deprecated and should be avoided
 */
public record PatternMeta(
        @Nullable String by,
        @Nullable String description,
        @NotNull List<String> examples,
        @Nullable String since,
        @Nullable Category category,
        boolean deprecated) {

    /**
     * A shared empty meta instance used when no documentation is provided.
     */
    public static final PatternMeta EMPTY = new PatternMeta(null, null, List.of(), null, null, false);
}
