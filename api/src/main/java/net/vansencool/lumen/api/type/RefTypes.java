package net.vansencool.lumen.api.type;

import org.jetbrains.annotations.NotNull;

/**
 * Built-in reference type constants that are always available in Lumen.
 *
 * <p>These correspond to the default Bukkit types that Lumen ships with.
 * Addons can use these directly when registering event definitions or defining
 * variables in handlers, rather than looking them up by id.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * api.events().register(
 *     api.events().builder("respawn")
 *         .className("org.bukkit.event.player.PlayerRespawnEvent")
 *         .addVar("player", RefTypes.PLAYER,
 *                 "org.bukkit.entity.Player", "event.getPlayer()")
 *         .build()
 * );
 * }</pre>
 *
 * @see RefTypeHandle
 * @see RefTypeRegistrar
 */
@SuppressWarnings("unused")
public final class RefTypes {

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
     * The {@code DATA} ref type ({@code net.vansencool.lumen.pipeline.java.compiled.DataInstance}).
     */
    public static final @NotNull RefTypeHandle DATA = fixed("DATA", "net.vansencool.lumen.pipeline.java.compiled.DataInstance");

    private RefTypes() {
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
