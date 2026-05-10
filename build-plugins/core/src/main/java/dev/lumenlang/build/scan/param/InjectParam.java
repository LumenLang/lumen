package dev.lumenlang.build.scan.param;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One parameter on an annotated handler that carries {@code @Inject}.
 *
 * @param sourceName   parameter name as declared in source (requires {@code -parameters})
 * @param overrideName explicit placeholder name from {@code @Inject("...")}, or null when the source name is used
 * @param javaType     declared parameter type as a JVMS field descriptor
 * @param slot         local variable slot index in the method's bytecode
 */
public record InjectParam(@NotNull String sourceName, @Nullable String overrideName, @NotNull String javaType,
                          int slot) {

    /**
     * Effective placeholder name: the override when present, otherwise the source name.
     */
    public @NotNull String placeholderName() {
        return overrideName != null && !overrideName.isEmpty() ? overrideName : sourceName;
    }
}
