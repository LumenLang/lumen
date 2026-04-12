package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Immutable documentation metadata for a Lumen type.
 *
 * @param description a human-readable description of what this type represents, or {@code null}
 * @param usageAsType an example showing how a value of this type looks in Lumen source, or {@code null} if not applicable
 * @param examples    zero or more Lumen script examples demonstrating usage
 */
public record LumenTypeMeta(
        @Nullable String description,
        @Nullable String usageAsType,
        @NotNull List<String> examples
) {

    /**
     * A shared empty meta instance used when no documentation is provided.
     */
    public static final LumenTypeMeta EMPTY = new LumenTypeMeta(null, null, List.of());
}
