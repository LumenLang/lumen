package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;

/**
 * Constants for Lumen's own built-in object types that are not Minecraft specific.
 *
 * <p>These types represent Lumen ecosystem constructs like data class instances.
 */
@SuppressWarnings("unused")
public final class BuiltinLumenTypes {

    public static final @NotNull ObjectType DATA = new ObjectType("DATA", "dev.lumenlang.lumen.pipeline.java.compiled.DataInstance");

    public static final @NotNull ObjectType LIST = new ObjectType("LIST", "java.util.List");

    public static final @NotNull ObjectType MAP = new ObjectType("MAP", "java.util.Map");

    private BuiltinLumenTypes() {
    }

    /**
     * Registers all built-in Lumen types into the {@link LumenTypeRegistry}.
     * Must be called during plugin initialization.
     */
    public static void registerAll() {
        LumenTypeRegistry.register(DATA.id(), DATA.javaType(), DATA.keyTemplate(), DATA.superType());
        LumenTypeRegistry.register(LIST.id(), LIST.javaType(), LIST.keyTemplate(), LIST.superType());
        LumenTypeRegistry.register(MAP.id(), MAP.javaType(), MAP.keyTemplate(), MAP.superType());
    }
}
