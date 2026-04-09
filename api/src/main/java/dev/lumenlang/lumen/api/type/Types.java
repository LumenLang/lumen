package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;

/**
 * Backward-compatible entry point for primitive type constants.
 *
 * <p>Delegates to {@link PrimitiveType} for all primitive types. For Minecraft types,
 * use {@link MinecraftTypes}. For built-in Lumen types, use {@link BuiltinLumenTypes}.
 *
 * @deprecated Use {@link PrimitiveType}, {@link MinecraftTypes}, or {@link BuiltinLumenTypes} directly.
 */
@Deprecated
@SuppressWarnings("unused")
public final class Types {

    public static final @NotNull String BOOLEAN = "boolean";

    public static final @NotNull String INT = "int";

    public static final @NotNull String LONG = "long";

    public static final @NotNull String DOUBLE = "double";

    public static final @NotNull String FLOAT = "float";

    public static final @NotNull String STRING = "String";

    private Types() {
    }
}
