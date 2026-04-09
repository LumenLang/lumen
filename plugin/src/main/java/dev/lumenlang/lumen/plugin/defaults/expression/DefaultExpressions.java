package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.Types;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Projectile;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Registers built-in expression patterns.
 */
@Registration
@SuppressWarnings("unused")
public final class DefaultExpressions {

    /**
     * Maps an {@code EntityType} constant to its corresponding Bukkit entity
     * class name and returns it as a metadata map with a {@code "javaClass"}
     * key. Falls back to {@code LivingEntity} when the concrete class cannot
     * be resolved.
     *
     * @param typeEnum the Java expression for the EntityType constant
     *                 (e.g. {@code "org.bukkit.entity.EntityType.ZOMBIE"})
     * @return metadata map containing the javaClass, or empty if unresolvable
     */
    private static @NotNull Map<String, Object> resolveEntityMeta(@NotNull String typeEnum) {
        String name = typeEnum.replace("EntityType.", "");
        try {
            EntityType et = EntityType.valueOf(name);
            Class<?> cls = et.getEntityClass();
            if (cls != null) {
                return Map.of("javaClass", cls.getName());
            }
        } catch (IllegalArgumentException ignored) {
        }
        return Map.of();
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        registerLocation(api);
        registerSpawn(api);
        registerProjectile(api);

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("none")
                .description("Represents a null (absent) value. Useful as a default for global vars that hold optional objects like locations or players.")
                .examples("global scoped pos1 with default none", "set result to none")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> new ExpressionResult("(Object) null", null)));

        registerTypedNulls(api);

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get location %who:PLAYER%")
                .description("Returns the current location of a player.")
                .example("set loc to get location player")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .returnType(MinecraftTypes.LOCATION.id())
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getLocation()",
                        MinecraftTypes.LOCATION.id())));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% location")
                .description("Returns the current location of a player.")
                .example("set loc to get player's location")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .returnType(MinecraftTypes.LOCATION.id())
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getLocation()",
                        MinecraftTypes.LOCATION.id())));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% world")
                .description("Returns the world a player is currently in.")
                .example("set w to get player's world")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .returnType(MinecraftTypes.WORLD.id())
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getWorld()",
                        MinecraftTypes.WORLD.id())));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get player by (name|username) %name:STRING%")
                .description("Looks up an online player by name.")
                .example("set p to get player by name \"Notch\"")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .returnType(MinecraftTypes.PLAYER.id())
                .handler(ctx -> new ExpressionResult(
                        "Bukkit.getPlayer(" + ctx.java("name") + ")",
                        MinecraftTypes.PLAYER.id())));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get player by (name|username) %name:EXPR%")
                .description("Looks up an online player by a name expression (variable or value).")
                .example("set p to get player by name target_name")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .returnType(MinecraftTypes.PLAYER.id())
                .handler(ctx -> new ExpressionResult(
                        "Bukkit.getPlayer(String.valueOf(" + ctx.java("name") + "))",
                        MinecraftTypes.PLAYER.id())));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get player by (uuid|unique id) %uuid:STRING%")
                .description("Looks up an online player by UUID string.")
                .example("set p to get player by uuid \"069a79f4-44e9-4726-a5be-fca90e38aaf5\"")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .returnType(MinecraftTypes.PLAYER.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(UUID.class.getName());
                    return new ExpressionResult(
                            "Bukkit.getPlayer(UUID.fromString(" + ctx.java("uuid") + "))",
                            MinecraftTypes.PLAYER.id());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get world %name:STRING%")
                .description("Looks up a world by name.")
                .example("set w to get world \"world_nether\"")
                .since("1.0.0")
                .category(Categories.WORLD)
                .returnType(MinecraftTypes.WORLD.id())
                .handler(ctx -> new ExpressionResult(
                        "Bukkit.getWorld(" + ctx.java("name") + ")",
                        MinecraftTypes.WORLD.id())));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get offline player by (name|username) %name:STRING%")
                .description("Looks up an offline player by name.")
                .example("set op to get offline player by name \"Notch\"")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .returnType(MinecraftTypes.OFFLINE_PLAYER.id())
                .handler(ctx -> {
                    String nameJava = ctx.java("name");
                    ctx.codegen().addImport(OfflinePlayer.class.getName());
                    return new ExpressionResult(
                            "Bukkit.getOfflinePlayer(" + nameJava + ")",
                            MinecraftTypes.OFFLINE_PLAYER.id());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get offline player by (uuid|unique id) %uuid:STRING%")
                .description("Looks up an offline player by UUID string.")
                .example("set op to get offline player by uuid \"069a79f4-44e9-4726-a5be-fca90e38aaf5\"")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .returnType(MinecraftTypes.OFFLINE_PLAYER.id())
                .handler(ctx -> {
                    String uuidJava = ctx.java("uuid");
                    ctx.codegen().addImport(OfflinePlayer.class.getName());
                    ctx.codegen().addImport(UUID.class.getName());
                    return new ExpressionResult(
                            "Bukkit.getOfflinePlayer(UUID.fromString("
                                    + uuidJava + "))",
                            MinecraftTypes.OFFLINE_PLAYER.id());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %op:PLAYER_POSSESSIVE% name")
                .description("Returns an online player's display name.")
                .example("set name to get player's name")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .returnType(Types.STRING)
                .handler(ctx -> new ExpressionResult(ctx.java("op") + ".getName()", Types.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %op:OFFLINE_PLAYER_POSSESSIVE% name")
                .description("Returns an offline player's name.")
                .example("set name to get offlinePlayer's name")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .returnType(Types.STRING)
                .handler(ctx -> new ExpressionResult(ctx.java("op") + ".getName()", Types.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %op:OFFLINE_PLAYER_POSSESSIVE% uuid")
                .description("Returns an offline player's UUID as a string.")
                .example("set id to get offlinePlayer's uuid")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .returnType(Types.STRING)
                .handler(ctx -> new ExpressionResult(ctx.java("op") + ".getUniqueId().toString()",
                        Types.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %op:OFFLINE_PLAYER_POSSESSIVE% first played")
                .description("Returns the timestamp of when the offline player first joined.")
                .example("set time to get offlinePlayer's first played")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .returnType(Types.LONG)
                .handler(ctx -> new ExpressionResult(ctx.java("op") + ".getFirstPlayed()", Types.LONG)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %op:OFFLINE_PLAYER_POSSESSIVE% last played")
                .description("Returns the timestamp of when the offline player last joined.")
                .example("set time to get offlinePlayer's last played")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .returnType(Types.LONG)
                .handler(ctx -> new ExpressionResult(ctx.java("op") + ".getLastPlayed()", Types.LONG)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %op:OFFLINE_PLAYER_POSSESSIVE% bed spawn location")
                .description("Returns the offline player's bed spawn location, or null if not set.")
                .example("set loc to get offlinePlayer's bed spawn location")
                .since("1.0.0")
                .category(Categories.OFFLINE_PLAYER)
                .returnType(MinecraftTypes.LOCATION.id())
                .handler(ctx -> new ExpressionResult(ctx.java("op") + ".getBedSpawnLocation()",
                        MinecraftTypes.LOCATION.id())));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% x")
                .description("Returns the player's current X coordinate as a double.")
                .example("set px to get player's x")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .returnType(Types.DOUBLE)
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getLocation().getX()", Types.DOUBLE)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% y")
                .description("Returns the player's current Y coordinate as a double.")
                .example("set py to get player's y")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .returnType(Types.DOUBLE)
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getLocation().getY()", Types.DOUBLE)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% z")
                .description("Returns the player's current Z coordinate as a double.")
                .example("set pz to get player's z")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .returnType(Types.DOUBLE)
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getLocation().getZ()", Types.DOUBLE)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% uuid")
                .description("Returns the player's UUID as a string.")
                .example("set id to get player's uuid")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .returnType(Types.STRING)
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getUniqueId().toString()",
                        Types.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% (xp level|level)")
                .description("Returns a player's experience level as an integer.")
                .example("set lv to get player's xp level")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .returnType(Types.INT)
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getLevel()",
                        Types.INT)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% eye location")
                .description("Returns the location of a player's eyes.")
                .example("set loc to get player's eye location")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .returnType(MinecraftTypes.LOCATION.id())
                .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getEyeLocation()",
                        MinecraftTypes.LOCATION.id())));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% direction x")
                .description("Returns the X component of the direction a player is looking.")
                .example("set dx to get player's direction x")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .returnType(Types.DOUBLE)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("who") + ".getLocation().getDirection().getX()", Types.DOUBLE)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% direction y")
                .description("Returns the Y component of the direction a player is looking.")
                .example("set dy to get player's direction y")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .returnType(Types.DOUBLE)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("who") + ".getLocation().getDirection().getY()", Types.DOUBLE)));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %who:PLAYER_POSSESSIVE% direction z")
                .description("Returns the Z component of the direction a player is looking.")
                .example("set dz to get player's direction z")
                .since("1.0.0")
                .category(Categories.PLAYER)
                .returnType(Types.DOUBLE)
                .handler(ctx -> new ExpressionResult(
                        ctx.java("who") + ".getLocation().getDirection().getZ()", Types.DOUBLE)));
    }

    /**
     * Registers location constructor expressions.
     *
     * <p>
     * Allows creating {@code Location} objects from coordinates:
     *
     * <pre>{@code
     * set loc to new location in myWorld at 100 64 -200
     * }</pre>
     */
    private void registerLocation(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("new location in %w:WORLD% at %x:INT% %y:INT% %z:INT%")
                .description("Creates a new Location from a world and XYZ coordinates.")
                .example("set loc to new location in myWorld at 100 64 -200")
                .since("1.0.0")
                .category(Categories.LOCATION)
                .returnType(MinecraftTypes.LOCATION.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(Location.class.getName());
                    return new ExpressionResult(
                            "new Location(" + ctx.java("w") + ", " + ctx.java("x") + ", "
                                    + ctx.java("y") + ", " + ctx.java("z") + ")",
                            MinecraftTypes.LOCATION.id());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("new location at %x:INT% %y:INT% %z:INT% in %w:WORLD%")
                .description("Creates a new Location from XYZ coordinates and a world.")
                .example("set loc to new location at 100 64 -200 in myWorld")
                .since("1.0.0")
                .category(Categories.LOCATION)
                .returnType(MinecraftTypes.LOCATION.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(Location.class.getName());
                    return new ExpressionResult(
                            "new Location(" + ctx.java("w") + ", " + ctx.java("x") + ", "
                                    + ctx.java("y") + ", " + ctx.java("z") + ")",
                            MinecraftTypes.LOCATION.id());
                }));
    }

    /**
     * Registers spawn as an expression so it can be used in variable assignment.
     *
     * <p>
     * This makes {@code set mob to spawn zombie at loc} work, returning the
     * spawned entity as an {@code ENTITY}-typed variable.
     */
    private void registerSpawn(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("spawn %type:ENTITY_TYPE% at %who:PLAYER%")
                .description("Spawns an entity at a player's location and returns it.")
                .example("set mob to spawn zombie at player")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .returnType(MinecraftTypes.ENTITY.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(Entity.class.getName());
                    String typeEnum = ctx.java("type");
                    String playerJava = ctx.java("who");
                    Map<String, Object> meta = resolveEntityMeta(typeEnum);
                    return new ExpressionResult(
                            playerJava + ".getLocation().getWorld().spawnEntity("
                                    + playerJava + ".getLocation(), " + typeEnum
                                    + ")",
                            MinecraftTypes.ENTITY.id(),
                            meta);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("spawn %type:ENTITY_TYPE% [at] %loc:LOCATION%")
                .description("Spawns an entity at a location and returns it.")
                .example("set mob to spawn zombie at myLoc")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .returnType(MinecraftTypes.ENTITY.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(Entity.class.getName());
                    String typeEnum = ctx.java("type");
                    Map<String, Object> meta = resolveEntityMeta(typeEnum);
                    return new ExpressionResult(
                            ctx.java("loc") + ".getWorld().spawnEntity(" + ctx.java("loc")
                                    + ", " + typeEnum + ")",
                            MinecraftTypes.ENTITY.id(),
                            meta);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("spawn %type:ENTITY_TYPE% at %loc:EXPR%")
                .description("Spawns an entity at an expression that resolves to a location and returns it.")
                .example("set mob to spawn zombie at get player's location")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .returnType(MinecraftTypes.ENTITY.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(Entity.class.getName());
                    String typeEnum = ctx.java("type");
                    String locJava = ctx.java("loc");
                    Map<String, Object> meta = resolveEntityMeta(typeEnum);
                    return new ExpressionResult(
                            locJava + ".getWorld().spawnEntity(" + locJava
                                    + ", " + typeEnum + ")",
                            MinecraftTypes.ENTITY.id(),
                            meta);
                }));
    }

    /**
     * Registers projectile launch expressions.
     *
     * <p>
     * Allows launching a projectile from a player in the direction they are looking:
     *
     * <pre>{@code
     * set proj to launch snowball from player
     * }</pre>
     *
     * @param api the Lumen API to register expressions on
     */
    private void registerProjectile(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("launch %type:ENTITY_TYPE% from %who:PLAYER%")
                .description("Launches a projectile from a player in the direction they are looking and returns it.")
                .example("set proj to launch snowball from player")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .returnType(MinecraftTypes.ENTITY.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(Entity.class.getName());
                    ctx.codegen().addImport(Projectile.class.getName());
                    String type = ctx.java("type");
                    String player = ctx.java("who");
                    return new ExpressionResult(
                            "(Entity) " + player + ".launchProjectile((Class) " + type + ".getEntityClass())",
                            MinecraftTypes.ENTITY.id());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("launch %type:ENTITY_TYPE% from %loc:LOCATION%")
                .description("Spawns a projectile at a location and returns it. The projectile spawns with no initial velocity.")
                .example("set proj to launch snowball from myLoc")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .returnType(MinecraftTypes.ENTITY.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(Entity.class.getName());
                    String type = ctx.java("type");
                    String loc = ctx.java("loc");
                    return new ExpressionResult(
                            loc + ".getWorld().spawnEntity(" + loc + ", " + type + ")",
                            MinecraftTypes.ENTITY.id());
                }));
    }

    /**
     * Registers typed null expressions ({@code no location}, {@code no player},
     * etc.)
     * that produce a {@code null} value tagged with the corresponding type.
     *
     * <p>
     * These allow global or persistent vars to carry compile-time type information
     * even when their initial value is absent:
     *
     * <pre>{@code
     * global scoped pos1 with default no location
     * }</pre>
     *
     * @param api the Lumen API to register expressions on
     */
    private void registerTypedNulls(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("no location")
                .description("Represents a null location value. The variable will carry the LOCATION type at compile time.")
                .example("global scoped pos1 with default no location")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .returnType(MinecraftTypes.LOCATION.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(Location.class.getName());
                    return new ExpressionResult("(Location) null", MinecraftTypes.LOCATION.id());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("no player")
                .description("Represents a null player value. The variable will carry the PLAYER type at compile time.")
                .example("global scoped target with default no player")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .returnType(MinecraftTypes.PLAYER.id())
                .handler(ctx -> new ExpressionResult("(Player) null", MinecraftTypes.PLAYER.id())));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("no entity")
                .description("Represents a null entity value. The variable will carry the ENTITY type at compile time.")
                .example("global scoped target with default no entity")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .returnType(MinecraftTypes.ENTITY.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(Entity.class.getName());
                    return new ExpressionResult("(Entity) null", MinecraftTypes.ENTITY.id());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("no world")
                .description("Represents a null world value. The variable will carry the WORLD type at compile time.")
                .example("global scoped w with default no world")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .returnType(MinecraftTypes.WORLD.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(World.class.getName());
                    return new ExpressionResult("(World) null", MinecraftTypes.WORLD.id());
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("no block")
                .description("Represents a null block value. The variable will carry the BLOCK type at compile time.")
                .example("global scoped target_block with default no block")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .returnType(MinecraftTypes.BLOCK.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(Block.class.getName());
                    return new ExpressionResult("(Block) null", MinecraftTypes.BLOCK.id());
                }));
    }
}
