package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;

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
    public static final @NotNull ObjectType LIVING_ENTITY = new ObjectType("LIVING_ENTITY", "org.bukkit.entity.LivingEntity", "$.getUniqueId().toString()", ENTITY);
    public static final @NotNull ObjectType PLAYER = new ObjectType("PLAYER", "org.bukkit.entity.Player", "$.getUniqueId().toString()", LIVING_ENTITY);
    public static final @NotNull ObjectType SENDER = new ObjectType("SENDER", "org.bukkit.command.CommandSender", "$.getName()");
    public static final @NotNull ObjectType LOCATION = new ObjectType("LOCATION", "org.bukkit.Location");
    public static final @NotNull ObjectType ITEMSTACK = new ObjectType("ITEMSTACK", "org.bukkit.inventory.ItemStack");
    public static final @NotNull ObjectType INVENTORY = new ObjectType("INVENTORY", "org.bukkit.inventory.Inventory");
    public static final @NotNull ObjectType WORLD = new ObjectType("WORLD", "org.bukkit.World", "$.getName()");
    public static final @NotNull ObjectType OFFLINE_PLAYER = new ObjectType("OFFLINE_PLAYER", "org.bukkit.OfflinePlayer", "$.getUniqueId().toString()");
    public static final @NotNull ObjectType BLOCK = new ObjectType("BLOCK", "org.bukkit.block.Block", "$.getLocation().toString()");

    private MinecraftTypes() {
    }

    /**
     * Registers all Minecraft types into the {@link LumenTypeRegistry}.
     * Must be called during plugin initialization.
     */
    public static void registerAll() {
        LumenTypeRegistry.register(ENTITY.id(), ENTITY.javaType(), ENTITY.keyTemplate(), ENTITY.superType());
        LumenTypeRegistry.register(LIVING_ENTITY.id(), LIVING_ENTITY.javaType(), LIVING_ENTITY.keyTemplate(), LIVING_ENTITY.superType());
        LumenTypeRegistry.register(PLAYER.id(), PLAYER.javaType(), PLAYER.keyTemplate(), PLAYER.superType());
        LumenTypeRegistry.register(SENDER.id(), SENDER.javaType(), SENDER.keyTemplate(), SENDER.superType());
        LumenTypeRegistry.register(LOCATION.id(), LOCATION.javaType(), LOCATION.keyTemplate(), LOCATION.superType());
        LumenTypeRegistry.register(ITEMSTACK.id(), ITEMSTACK.javaType(), ITEMSTACK.keyTemplate(), ITEMSTACK.superType());
        LumenTypeRegistry.register(INVENTORY.id(), INVENTORY.javaType(), INVENTORY.keyTemplate(), INVENTORY.superType());
        LumenTypeRegistry.register(WORLD.id(), WORLD.javaType(), WORLD.keyTemplate(), WORLD.superType());
        LumenTypeRegistry.register(OFFLINE_PLAYER.id(), OFFLINE_PLAYER.javaType(), OFFLINE_PLAYER.keyTemplate(), OFFLINE_PLAYER.superType());
        LumenTypeRegistry.register(BLOCK.id(), BLOCK.javaType(), BLOCK.keyTemplate(), BLOCK.superType());
    }
}
