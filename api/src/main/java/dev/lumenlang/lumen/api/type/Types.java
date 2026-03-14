package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;

/**
 * Unified entry point for all built-in Lumen type constants.
 *
 * <h2>Reference Types</h2>
 * <p>Reference types carry a {@link RefTypeHandle} which provides an id, a Java class
 * name, and a key expression for lookups. These are used for Bukkit object types such
 * as players, entities, locations, and items.
 *
 * <h2>Primitive Types</h2>
 * <p>Primitive type constants are plain {@code String} values representing Java types.
 * These are used for numeric values, booleans, and strings in event variable
 * definitions and expression results.
 *
 * <h2>Event Variable Registration</h2>
 * <pre>{@code
 * api.events().register(api.events().builder("entity_damage")
 *     .className("org.bukkit.event.entity.EntityDamageEvent")
 *     .addVar("entity", Types.ENTITY, "event.getEntity()")
 *     .addVar("damage", Types.DOUBLE, "event.getDamage()")
 *     .build()
 * );
 * }</pre>
 *
 * <h2>Custom Type Registration</h2>
 * <p>Addons can register entirely new reference types via {@link RefTypeRegistrar}.
 * Once registered, these can be used in event definitions, variable declarations,
 * and placeholder bindings just like the built-in types.
 *
 * @see RefTypeHandle
 * @see RefTypeRegistrar
 * @see TypeRegistrar
 */
@SuppressWarnings("unused")
public final class Types {

    /**
     * The {@code PLAYER} ref type ({@code org.bukkit.entity.Player}).
     */
    public static final @NotNull RefTypeHandle PLAYER = fixed("PLAYER", "org.bukkit.entity.Player");

    /**
     * The {@code SENDER} ref type ({@code org.bukkit.command.CommandSender}).
     */
    public static final @NotNull RefTypeHandle SENDER = fixed("SENDER", "org.bukkit.command.CommandSender");

    /**
     * The {@code LOCATION} ref type ({@code org.bukkit.Location}).
     */
    public static final @NotNull RefTypeHandle LOCATION = fixed("LOCATION", "org.bukkit.Location");

    /**
     * The {@code ITEMSTACK} ref type ({@code org.bukkit.inventory.ItemStack}).
     */
    public static final @NotNull RefTypeHandle ITEMSTACK = fixed("ITEMSTACK", "org.bukkit.inventory.ItemStack");

    /**
     * The {@code INVENTORY} ref type ({@code org.bukkit.inventory.Inventory}).
     */
    public static final @NotNull RefTypeHandle INVENTORY = fixed("INVENTORY", "org.bukkit.inventory.Inventory");

    /**
     * The {@code WORLD} ref type ({@code org.bukkit.World}).
     */
    public static final @NotNull RefTypeHandle WORLD = fixed("WORLD", "org.bukkit.World");

    /**
     * The {@code OFFLINE_PLAYER} ref type ({@code org.bukkit.OfflinePlayer}).
     */
    public static final @NotNull RefTypeHandle OFFLINE_PLAYER = fixed("OFFLINE_PLAYER", "org.bukkit.OfflinePlayer");

    /**
     * The {@code LIST} ref type ({@code java.util.List}).
     */
    public static final @NotNull RefTypeHandle LIST = fixed("LIST", "java.util.List");

    /**
     * The {@code MAP} ref type ({@code java.util.Map}).
     */
    public static final @NotNull RefTypeHandle MAP = fixed("MAP", "java.util.Map");

    /**
     * The {@code ENTITY} ref type ({@code org.bukkit.entity.Entity}).
     */
    public static final @NotNull RefTypeHandle ENTITY = fixed("ENTITY", "org.bukkit.entity.Entity");

    /**
     * The {@code BLOCK} ref type ({@code org.bukkit.block.Block}).
     */
    public static final @NotNull RefTypeHandle BLOCK = fixed("BLOCK", "org.bukkit.block.Block");

    /**
     * The {@code DATA} ref type ({@code dev.lumenlang.lumen.pipeline.java.compiled.DataInstance}).
     */
    public static final @NotNull RefTypeHandle DATA = fixed("DATA", "dev.lumenlang.lumen.pipeline.java.compiled.DataInstance");

    /**
     * The {@code boolean} primitive type.
     */
    public static final @NotNull String BOOLEAN = "boolean";

    /**
     * The {@code int} primitive type.
     */
    public static final @NotNull String INT = "int";

    /**
     * The {@code long} primitive type.
     */
    public static final @NotNull String LONG = "long";

    /**
     * The {@code double} primitive type.
     */
    public static final @NotNull String DOUBLE = "double";

    /**
     * The {@code float} primitive type.
     */
    public static final @NotNull String FLOAT = "float";

    /**
     * The {@code String} type.
     */
    public static final @NotNull String STRING = "String";

    private Types() {
    }

    private static @NotNull RefTypeHandle fixed(@NotNull String id, @NotNull String javaType) {
        return new RefTypeHandle() {
            @Override
            public @NotNull String id() {
                return id;
            }

            @Override
            public @NotNull String javaType() {
                return javaType;
            }

            @Override
            public @NotNull String keyExpression(@NotNull String javaVar) {
                return "String.valueOf(" + javaVar + ")";
            }

            @Override
            public String toString() {
                return id;
            }
        };
    }
}
