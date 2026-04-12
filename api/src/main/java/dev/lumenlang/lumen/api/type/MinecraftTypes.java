package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Constants for Minecraft/Bukkit specific object types.
 *
 * <p>These types must be registered into the {@link LumenTypeRegistry} during plugin
 * initialization before they can be used in type resolution. The constants here provide
 * a convenient way to reference common Minecraft types from pattern handlers and event
 * definitions.
 */
@SuppressWarnings("unused")
public final class MinecraftTypes {

    public static final @NotNull ObjectType ENTITY = new ObjectType("ENTITY", "org.bukkit.entity.Entity", "$.getUniqueId().toString()");
    public static final @NotNull ObjectType LIVING_ENTITY = new ObjectType("LIVING_ENTITY", "org.bukkit.entity.LivingEntity", "$.getUniqueId().toString()", List.of(ENTITY));
    public static final @NotNull ObjectType PLAYER = new ObjectType("PLAYER", "org.bukkit.entity.Player", "$.getUniqueId().toString()", List.of(LIVING_ENTITY));
    public static final @NotNull ObjectType SENDER = new ObjectType("SENDER", "org.bukkit.command.CommandSender", "$.getName()");
    public static final @NotNull ObjectType LOCATION = new ObjectType("LOCATION", "org.bukkit.Location");
    public static final @NotNull ObjectType ITEMSTACK = new ObjectType("ITEMSTACK", "org.bukkit.inventory.ItemStack");
    public static final @NotNull ObjectType INVENTORY = new ObjectType("INVENTORY", "org.bukkit.inventory.Inventory");
    public static final @NotNull ObjectType WORLD = new ObjectType("WORLD", "org.bukkit.World", "$.getName()");
    public static final @NotNull ObjectType OFFLINE_PLAYER = new ObjectType("OFFLINE_PLAYER", "org.bukkit.OfflinePlayer", "$.getUniqueId().toString()");
    public static final @NotNull ObjectType BLOCK = new ObjectType("BLOCK", "org.bukkit.block.Block", "$.getLocation().toString()");
    public static final @NotNull ObjectType VECTOR = new ObjectType("VECTOR", "org.bukkit.util.Vector");
    public static final @NotNull ObjectType ENTITY_TYPE = new ObjectType("ENTITY_TYPE", "org.bukkit.entity.EntityType");

    private MinecraftTypes() {
    }

    /**
     * Registers all Minecraft types into the {@link LumenTypeRegistry}.
     * Must be called during plugin initialization.
     */
    public static void registerAll() {
        LumenTypeRegistry.register(ENTITY);
        LumenTypeRegistry.register(LIVING_ENTITY);
        LumenTypeRegistry.register(PLAYER);
        LumenTypeRegistry.register(SENDER);
        LumenTypeRegistry.register(LOCATION);
        LumenTypeRegistry.register(ITEMSTACK);
        LumenTypeRegistry.register(INVENTORY);
        LumenTypeRegistry.register(WORLD);
        LumenTypeRegistry.register(OFFLINE_PLAYER);
        LumenTypeRegistry.register(BLOCK);
        LumenTypeRegistry.register(VECTOR);
        LumenTypeRegistry.register(ENTITY_TYPE);
    }
}
