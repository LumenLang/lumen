package dev.lumenlang.build.scan.handler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Optional documentation metadata captured from a handler's annotations.
 *
 * @param description value of {@code @Description}, or null
 * @param examples    values of one or more {@code @Example} (empty when none)
 * @param since       value of {@code @Since}, or null
 * @param category    value of {@code @Category}, or null
 * @param deprecated  whether {@link Deprecated} is present
 */
public record HandlerMeta(@Nullable String description, @NotNull List<String> examples, @Nullable String since, @Nullable String category, boolean deprecated) {

    public static final HandlerMeta EMPTY = new HandlerMeta(null, List.of(), null, null, false);
}
